package com.botica.inteligente.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.categoria.service.CategoriaComando;
import com.botica.inteligente.categoria.service.CategoriaResumen;
import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.laboratorio.service.LaboratorioComando;
import com.botica.inteligente.laboratorio.service.LaboratorioResumen;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.producto.service.ProductoComando;
import com.botica.inteligente.producto.service.ProductoService;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Humo de las pantallas del catalogo: carga el FXML real y verifica que la tabla se llena. */
@SpringBootTest
class VistasCatalogoTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();
    private static boolean toolkitDisponible;

    @Autowired
    private ApplicationContext contexto;
    @Autowired
    private CategoriaService categoriaService;
    @Autowired
    private LaboratorioService laboratorioService;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private LaboratorioRepository laboratorioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SesionActual sesionActual;

    @DynamicPropertySource
    static void configureTemporarySqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMPORARY_DATABASE);
    }

    @BeforeAll
    static void iniciarJavaFx() {
        try {
            Platform.startup(() -> { });
            toolkitDisponible = true;
        } catch (IllegalStateException yaIniciado) {
            toolkitDisponible = true;
        } catch (UnsupportedOperationException | UnsatisfiedLinkError sinPantalla) {
            toolkitDisponible = false;
        }
    }

    @BeforeEach
    void prepararDatos() {
        assumeTrue(toolkitDisponible, "JavaFX no disponible en este entorno");
        sesionActual.cerrarSesion();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        laboratorioRepository.deleteAll();
        usuarioRepository.deleteAll();
        Usuario owner = new Usuario();
        owner.setUsername("owner");
        owner.setPassword("$2a$10$no-se-usa");
        owner.setRoles(Rol.OWNER);
        owner.setEstado(true);
        sesionActual.autenticar(usuarioRepository.saveAndFlush(owner));

        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Analgésicos", null));
        LaboratorioResumen laboratorio = laboratorioService.crear(
            new LaboratorioComando("Lab Uno", null, null, null, null, null)
        );
        productoService.crear(new ProductoComando(
            "P-001", null, "Paracetamol 500 mg", null, null, TipoProducto.MEDICAMENTO, null, null, null,
            null, null, null, null, null, null, false, null,
            new BigDecimal("1.00"), new BigDecimal("2.00"), 5, categoria.id(), laboratorio.id(), null
        ));
    }

    @Test
    void categoriasCargaLaTabla() throws Exception {
        assertEquals(1, filasTrasCargar("/view/categorias-view.fxml"));
    }

    @Test
    void laboratoriosCargaLaTabla() throws Exception {
        assertEquals(1, filasTrasCargar("/view/laboratorios-view.fxml"));
    }

    @Test
    void productosCargaLaTabla() throws Exception {
        assertEquals(1, filasTrasCargar("/view/productos-view.fxml"));
    }

    @Test
    void inventarioYComprasCarganSinErrores() throws Exception {
        for (String ruta : new String[] {"/view/inventario-view.fxml", "/view/compras-view.fxml"}) {
            CompletableFuture<Object> controlador = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
                    loader.setControllerFactory(contexto::getBean);
                    loader.load();
                    Object c = loader.getController();
                    ((Refrescable) c).refrescar();
                    controlador.complete(c);
                } catch (IOException | RuntimeException error) {
                    controlador.completeExceptionally(error);
                }
            });
            assertNotNull(controlador.get(10, TimeUnit.SECONDS));
            Thread.sleep(500);
        }
    }

    private int filasTrasCargar(String ruta) throws Exception {
        CompletableFuture<TableView<?>> tabla = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
                loader.setControllerFactory(contexto::getBean);
                Parent raiz = loader.load();
                CatalogoController<?, ?> controlador = loader.getController();
                controlador.refrescar();
                tabla.complete(controlador.tabla);
                raiz.getId();
            } catch (IOException | RuntimeException error) {
                tabla.completeExceptionally(error);
            }
        });
        TableView<?> vista = tabla.get(10, TimeUnit.SECONDS);
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < limite) {
            int filas = enHiloFx(() -> vista.getItems().size());
            if (filas > 0) {
                return filas;
            }
            Thread.sleep(100);
        }
        return 0;
    }

    private <T> T enHiloFx(Supplier<T> accion) throws Exception {
        CompletableFuture<T> resultado = new CompletableFuture<>();
        Platform.runLater(() -> resultado.complete(accion.get()));
        return resultado.get(5, TimeUnit.SECONDS);
    }

    private static Path createTemporaryDatabasePath() {
        try {
            return Files.createTempDirectory("botica-vistas-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
