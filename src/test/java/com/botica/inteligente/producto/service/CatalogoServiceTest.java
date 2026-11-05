package com.botica.inteligente.producto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class CatalogoServiceTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();
    private static final String EAN_VALIDO = "4006381333931";

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

    private Usuario owner;
    private Usuario seller;

    @DynamicPropertySource
    static void configureTemporarySqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMPORARY_DATABASE);
    }

    @BeforeEach
    void prepararDatos() {
        sesionActual.cerrarSesion();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        laboratorioRepository.deleteAll();
        usuarioRepository.deleteAll();
        owner = guardarUsuario("owner", Rol.OWNER);
        seller = guardarUsuario("vendedor", Rol.SELLER);
        sesionActual.autenticar(owner);
    }

    // ---- Categorias ----

    @Test
    void categoriaRechazaNombreDuplicadoSinImportarMayusculas() {
        categoriaService.crear(new CategoriaComando("Analgésicos", null));

        CampoInvalidoException error = assertThrows(
            CampoInvalidoException.class,
            () -> categoriaService.crear(new CategoriaComando("  analgésicos ", "otra"))
        );
        assertEquals("nombre", error.getCampo());
    }

    @Test
    void categoriaNoSePuedeDesactivarConProductosActivosPeroSiSinEllos() {
        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Vitaminas", null));
        LaboratorioResumen laboratorio = laboratorioService.crear(laboratorio("Lab Uno", null));
        ProductoResumen producto = productoService
            .crear(producto("P-001", null, categoria.id(), laboratorio.id())).producto();

        NegocioException error = assertThrows(
            NegocioException.class, () -> categoriaService.cambiarEstado(categoria.id(), false)
        );
        assertTrue(error.getMessage().contains("productos activos"));

        productoService.cambiarEstado(producto.id(), false);
        assertFalse(categoriaService.cambiarEstado(categoria.id(), false).estado());
    }

    @Test
    void categoriaFiltraPorTextoYEstadoYPagina() {
        for (int i = 1; i <= 5; i++) {
            categoriaService.crear(new CategoriaComando("Categoria " + i, null));
        }
        CategoriaResumen otra = categoriaService.crear(new CategoriaComando("Otra", null));
        categoriaService.cambiarEstado(otra.id(), false);

        PageRequest pagina = PageRequest.of(0, 2, Sort.by("nombre"));
        Page<CategoriaResumen> porTexto = categoriaService.listar("categoria", null, pagina);
        assertEquals(5, porTexto.getTotalElements());
        assertEquals(2, porTexto.getContent().size());
        assertEquals(3, porTexto.getTotalPages());

        Page<CategoriaResumen> inactivas = categoriaService.listar(null, false, pagina);
        assertEquals(1, inactivas.getTotalElements());
        assertEquals("Otra", inactivas.getContent().get(0).nombre());
    }

    @Test
    void sellerPuedeListarPeroNoModificarCategorias() {
        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Antibióticos", null));
        sesionActual.autenticar(seller);

        assertEquals(1, categoriaService.listar(null, null, PageRequest.of(0, 10)).getTotalElements());
        assertThrows(NegocioException.class, () -> categoriaService.crear(new CategoriaComando("Nueva", null)));
        assertThrows(
            NegocioException.class,
            () -> categoriaService.actualizar(categoria.id(), new CategoriaComando("Otro", null))
        );
        assertThrows(NegocioException.class, () -> categoriaService.cambiarEstado(categoria.id(), false));
    }

    @Test
    void listarExigeSesionIniciada() {
        sesionActual.cerrarSesion();

        assertThrows(
            NegocioException.class, () -> categoriaService.listar(null, null, PageRequest.of(0, 10))
        );
    }

    // ---- Laboratorios ----

    @Test
    void laboratorioValidaRucCorreoYUnicidad() {
        laboratorioService.crear(laboratorio("Lab Uno", "20123456789"));

        assertEquals("ruc", assertThrows(
            CampoInvalidoException.class, () -> laboratorioService.crear(laboratorio("Lab Dos", "123"))
        ).getCampo());
        assertEquals("ruc", assertThrows(
            CampoInvalidoException.class,
            () -> laboratorioService.crear(laboratorio("Lab Tres", "20123456789"))
        ).getCampo());
        assertEquals("nombre", assertThrows(
            CampoInvalidoException.class, () -> laboratorioService.crear(laboratorio("LAB UNO", null))
        ).getCampo());
        assertEquals("correo", assertThrows(
            CampoInvalidoException.class,
            () -> laboratorioService.crear(new LaboratorioComando("Lab Cuatro", null, null, "no-es-correo", null, null))
        ).getCampo());
    }

    @Test
    void laboratorioNoSePuedeDesactivarConProductosActivos() {
        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Cat", null));
        LaboratorioResumen laboratorio = laboratorioService.crear(laboratorio("Lab Uno", null));
        productoService.crear(producto("P-001", null, categoria.id(), laboratorio.id()));

        assertThrows(NegocioException.class, () -> laboratorioService.cambiarEstado(laboratorio.id(), false));
    }

    @Test
    void sellerNoPuedeModificarLaboratorios() {
        sesionActual.autenticar(seller);

        assertThrows(NegocioException.class, () -> laboratorioService.crear(laboratorio("Lab", null)));
    }

    // ---- Productos ----

    @Test
    void productoValidaEan13UnicidadYPrecios() {
        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Cat", null));
        LaboratorioResumen laboratorio = laboratorioService.crear(laboratorio("Lab", null));
        productoService.crear(producto("P-001", EAN_VALIDO, categoria.id(), laboratorio.id()));

        assertEquals("codigoBarras", assertThrows(
            CampoInvalidoException.class,
            () -> productoService.crear(producto("P-002", "4006381333932", categoria.id(), laboratorio.id()))
        ).getCampo());
        assertEquals("codigoBarras", assertThrows(
            CampoInvalidoException.class,
            () -> productoService.crear(producto("P-003", EAN_VALIDO, categoria.id(), laboratorio.id()))
        ).getCampo());
        assertEquals("codigoInterno", assertThrows(
            CampoInvalidoException.class,
            () -> productoService.crear(producto("p-001", null, categoria.id(), laboratorio.id()))
        ).getCampo());

        ProductoComando precioNegativo = conPrecios(
            producto("P-004", null, categoria.id(), laboratorio.id()), new BigDecimal("-1"), BigDecimal.ONE
        );
        assertEquals("precioCompra", assertThrows(
            CampoInvalidoException.class, () -> productoService.crear(precioNegativo)
        ).getCampo());
    }

    @Test
    void productoAdvierteSiPrecioVentaEsMenorQueCompraSinBloquear() {
        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Cat", null));
        LaboratorioResumen laboratorio = laboratorioService.crear(laboratorio("Lab", null));
        ProductoComando comando = conPrecios(
            producto("P-001", null, categoria.id(), laboratorio.id()), new BigDecimal("10"), new BigDecimal("8")
        );

        ProductoResultado resultado = productoService.crear(comando);

        assertNotNull(resultado.advertencia());
        assertEquals(new BigDecimal("8.00"), resultado.producto().datos().precioVenta());
        ProductoResultado sinAdvertencia = productoService.actualizar(
            resultado.producto().id(), conPrecios(comando, new BigDecimal("10"), new BigDecimal("12"))
        );
        assertNull(sinAdvertencia.advertencia());
    }

    @Test
    void productoExigeCategoriaYLaboratorioActivos() {
        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Cat", null));
        LaboratorioResumen laboratorio = laboratorioService.crear(laboratorio("Lab", null));
        categoriaService.cambiarEstado(categoria.id(), false);

        assertEquals("categoria", assertThrows(
            CampoInvalidoException.class,
            () -> productoService.crear(producto("P-001", null, categoria.id(), laboratorio.id()))
        ).getCampo());
    }

    @Test
    void productoFiltraPorCategoriaLaboratorioTextoYBuscaPorCodigoDeBarras() {
        CategoriaResumen cat1 = categoriaService.crear(new CategoriaComando("Cat1", null));
        CategoriaResumen cat2 = categoriaService.crear(new CategoriaComando("Cat2", null));
        LaboratorioResumen lab = laboratorioService.crear(laboratorio("Lab", null));
        productoService.crear(producto("P-001", EAN_VALIDO, cat1.id(), lab.id()));
        productoService.crear(producto("P-002", null, cat2.id(), lab.id()));
        PageRequest pagina = PageRequest.of(0, 10, Sort.by("nombreComercial"));

        assertEquals(2, productoService.listar(ProductoFiltro.vacio(), pagina).getTotalElements());
        assertEquals(1, productoService.listar(
            new ProductoFiltro(null, null, cat1.id(), null, null), pagina).getTotalElements());
        assertEquals(1, productoService.listar(
            new ProductoFiltro("p-002", null, null, lab.id(), TipoProducto.MEDICAMENTO), pagina).getTotalElements());
        assertTrue(productoService.buscarPorCodigoBarras(EAN_VALIDO).isPresent());
        assertTrue(productoService.buscarPorCodigoBarras("0000000000000").isEmpty());
    }

    @Test
    void sellerPuedeListarPeroNoCrearNiCambiarEstadoDeProductos() {
        CategoriaResumen categoria = categoriaService.crear(new CategoriaComando("Cat", null));
        LaboratorioResumen laboratorio = laboratorioService.crear(laboratorio("Lab", null));
        ProductoResumen creado = productoService
            .crear(producto("P-001", null, categoria.id(), laboratorio.id())).producto();
        sesionActual.autenticar(seller);

        assertEquals(1, productoService.listar(ProductoFiltro.vacio(), PageRequest.of(0, 10)).getTotalElements());
        assertThrows(
            NegocioException.class,
            () -> productoService.crear(producto("P-002", null, categoria.id(), laboratorio.id()))
        );
        assertThrows(NegocioException.class, () -> productoService.cambiarEstado(creado.id(), false));
    }

    // ---- utilidades ----

    private LaboratorioComando laboratorio(String nombre, String ruc) {
        return new LaboratorioComando(nombre, ruc, null, null, null, null);
    }

    private ProductoComando producto(String codigo, String barras, Long categoriaId, Long laboratorioId) {
        return new ProductoComando(
            codigo, barras, "Producto " + codigo, null, null, TipoProducto.MEDICAMENTO, null, null, null,
            null, null, null, null, null, null, false, null,
            new BigDecimal("5.00"), new BigDecimal("8.00"), 5, categoriaId, laboratorioId, null
        );
    }

    private ProductoComando conPrecios(ProductoComando base, BigDecimal compra, BigDecimal venta) {
        return new ProductoComando(
            base.codigoInterno(), base.codigoBarras(), base.nombreComercial(), base.nombreGenerico(),
            base.descripcion(), base.tipoProducto(), base.principioActivo(), base.concentracion(),
            base.presentacion(), base.viaAdministracion(), base.indicacionesOficiales(),
            base.contraindicaciones(), base.advertencias(), base.precauciones(),
            base.condicionesAlmacenamiento(), base.requiereReceta(), base.registroSanitario(),
            compra, venta, base.stockMinimo(), base.categoriaId(), base.laboratorioId(),
            base.fuenteInformacion()
        );
    }

    private Usuario guardarUsuario(String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword("$2a$10$no-se-usa-en-estas-pruebas");
        usuario.setRoles(rol);
        usuario.setEstado(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private static Path createTemporaryDatabasePath() {
        try {
            return Files.createTempDirectory("botica-catalogo-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
