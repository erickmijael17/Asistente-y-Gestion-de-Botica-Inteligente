package com.botica.inteligente.asistente.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.inventario.entity.Lote;
import com.botica.inteligente.inventario.repository.LoteRepository;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class AsistenteServiceTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();

    @Autowired
    private AsistenteService asistente;
    @Autowired
    private ProductoRepository productoRepository;
    @Autowired
    private LoteRepository loteRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private LaboratorioRepository laboratorioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private SesionActual sesionActual;

    private Categoria categoria;
    private Laboratorio laboratorio;

    @DynamicPropertySource
    static void configureTemporarySqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMPORARY_DATABASE);
    }

    @BeforeEach
    void prepararCatalogo() {
        sesionActual.cerrarSesion();
        loteRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        laboratorioRepository.deleteAll();
        usuarioRepository.deleteAll();
        Usuario vendedor = new Usuario();
        vendedor.setUsername("vendedor");
        vendedor.setPassword("$2a$10$no-se-usa");
        vendedor.setRoles(Rol.SELLER);
        vendedor.setEstado(true);
        sesionActual.autenticar(usuarioRepository.saveAndFlush(vendedor));

        categoria = new Categoria();
        categoria.setNombre("Analgésicos");
        categoria = categoriaRepository.saveAndFlush(categoria);
        laboratorio = new Laboratorio();
        laboratorio.setNombre("Lab Uno");
        laboratorio = laboratorioRepository.saveAndFlush(laboratorio);

        Producto panadol = producto("P-1", "Panadol", "Paracetamol", "500 mg", "alivio de la fiebre y del dolor leve", false, "6.50");
        Producto paracetamolGenerico = producto("P-2", "Paracetamol Genérico", "Paracetamol", "500 mg", "alivio de la fiebre y del dolor leve", false, "2.50");
        Producto ibuprofeno = producto("P-3", "Ibuprofeno", "Ibuprofeno", "400 mg", "alivio del dolor, la inflamación y la fiebre", false, "4.00");
        Producto loratadina = producto("P-4", "Loratadina", "Loratadina", "10 mg", "alivio de los síntomas de la alergia", false, "3.00");
        Producto amoxicilina = producto("P-5", "Amoxicilina", "Amoxicilina", "500 mg", "tratamiento de infecciones bacterianas", true, "9.00");
        stock(panadol, 10);
        stock(paracetamolGenerico, 0);
        stock(ibuprofeno, 8);
        stock(loratadina, 5);
        stock(amoxicilina, 4);
    }

    @Test
    void sintomaDevuelveVariasOpcionesConDistintoPrincipioActivo() {
        RespuestaAsistente respuesta = asistente.consultar("pastillas para la fiebre");

        List<String> nombres = respuesta.sugerencias().stream().map(Sugerencia::nombre).toList();
        // mismo puntaje y con stock: primero el mas barato; el paracetamol repetido y sin stock va al final
        assertEquals(List.of("Ibuprofeno", "Panadol", "Paracetamol Genérico"), nombres);
        assertFalse(nombres.contains("Loratadina"));
        assertEquals(RespuestaAsistente.AVISO_ORIENTATIVO, respuesta.aviso());
    }

    @Test
    void ordenaPrimeroLosProductosConStock() {
        RespuestaAsistente respuesta = asistente.consultar("fiebre");

        Sugerencia ultimo = respuesta.sugerencias().get(respuesta.sugerencias().size() - 1);
        assertEquals("Paracetamol Genérico", ultimo.nombre());
        assertFalse(ultimo.hayStock());
        assertTrue(respuesta.sugerencias().get(0).hayStock());
    }

    @Test
    void entiendeAcentosYMayusculas() {
        RespuestaAsistente respuesta = asistente.consultar("ALERGÍA");

        assertEquals("Loratadina", respuesta.sugerencias().get(0).nombre());
    }

    @Test
    void alternativasDevuelveOtrasMarcasDelMismoPrincipioActivo() {
        RespuestaAsistente respuesta = asistente.consultar("alternativas a Panadol");

        assertEquals(1, respuesta.sugerencias().size());
        assertEquals("Paracetamol Genérico", respuesta.sugerencias().get(0).nombre());
        assertTrue(respuesta.texto().contains("mismo principio activo"));
    }

    @Test
    void preguntaPorUnMedicamentoDevuelveSuFichaYAvisaSiRequiereReceta() {
        RespuestaAsistente respuesta = asistente.consultar("para qué sirve la amoxicilina");

        assertEquals(1, respuesta.sugerencias().size());
        Sugerencia ficha = respuesta.sugerencias().get(0);
        assertEquals("Amoxicilina", ficha.nombre());
        assertTrue(ficha.requiereReceta());
        assertTrue(respuesta.texto().contains("infecciones bacterianas"));
        assertTrue(respuesta.texto().contains("Requiere receta"));
    }

    @Test
    void sinCoincidenciasExplicaQueNoEncontroNada() {
        RespuestaAsistente respuesta = asistente.consultar("xyzabc");

        assertTrue(respuesta.sugerencias().isEmpty());
        assertTrue(respuesta.texto().contains("No encontré"));
        assertEquals(RespuestaAsistente.AVISO_ORIENTATIVO, respuesta.aviso());
    }

    @Test
    void consultaVaciaOSoloPalabrasVaciasPideMasDetalle() {
        assertTrue(asistente.consultar("   ").sugerencias().isEmpty());
        assertTrue(asistente.consultar("para que el").texto().contains("No entendí"));
    }

    @Test
    void ignoraProductosInactivosYRequiereSesion() {
        Producto ibuprofeno = productoRepository.findAll().stream()
            .filter(p -> p.getNombreComercial().equals("Ibuprofeno")).findFirst().orElseThrow();
        ibuprofeno.setEstado(false);
        productoRepository.saveAndFlush(ibuprofeno);

        List<String> nombres = asistente.consultar("fiebre").sugerencias().stream().map(Sugerencia::nombre).toList();
        assertFalse(nombres.contains("Ibuprofeno"));

        sesionActual.cerrarSesion();
        assertThrows(NegocioException.class, () -> asistente.consultar("fiebre"));
    }

    // ---- utilidades ----

    private Producto producto(
        String codigo, String nombre, String principio, String concentracion, String indicaciones,
        boolean receta, String precio
    ) {
        Producto p = new Producto();
        p.setCodigoInterno(codigo);
        p.setNombreComercial(nombre);
        p.setPrincipioActivo(principio);
        p.setConcentracion(concentracion);
        p.setIndicacionesOficiales(indicaciones);
        p.setRequiereReceta(receta);
        p.setTipoProducto(TipoProducto.MEDICAMENTO);
        p.setPrecioCompra(new BigDecimal("1.00"));
        p.setPrecioVenta(new BigDecimal(precio));
        p.setStockMinimo(2);
        p.setCategoria(categoria);
        p.setLaboratorio(laboratorio);
        return productoRepository.saveAndFlush(p);
    }

    private void stock(Producto producto, int cantidad) {
        if (cantidad == 0) {
            return;
        }
        Lote lote = new Lote();
        lote.setProducto(producto);
        lote.setCodigoLote("L-" + producto.getCodigoInterno());
        lote.setFechaVencimiento(LocalDate.now().plusDays(200));
        lote.setCantidadInicial(cantidad);
        lote.setCantidadActual(cantidad);
        lote.setCostoUnitario(new BigDecimal("1.0000"));
        loteRepository.saveAndFlush(lote);
    }

    private static Path createTemporaryDatabasePath() {
        try {
            return Files.createTempDirectory("botica-asistente-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
