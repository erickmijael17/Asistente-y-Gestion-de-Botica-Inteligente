package com.botica.inteligente.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import com.botica.inteligente.compra.repository.CompraRepository;
import com.botica.inteligente.compra.service.CompraService;
import com.botica.inteligente.compra.service.LineaCompra;
import com.botica.inteligente.compra.service.NuevaCompra;
import com.botica.inteligente.inventario.repository.LoteRepository;
import com.botica.inteligente.inventario.repository.MovimientoInventarioRepository;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.proveedor.repository.ProveedorRepository;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import com.botica.inteligente.venta.enums.TipoComprobante;
import com.botica.inteligente.venta.repository.VentaRepository;
import com.botica.inteligente.venta.service.LineaVenta;
import com.botica.inteligente.venta.service.NuevaVenta;
import com.botica.inteligente.venta.service.VentaResumen;
import com.botica.inteligente.venta.service.VentaService;
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
class DashboardServiceTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private VentaService ventaService;
    @Autowired
    private CompraService compraService;
    @Autowired
    private VentaRepository ventaRepository;
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private MovimientoInventarioRepository movimientoRepository;
    @Autowired
    private LoteRepository loteRepository;
    @Autowired
    private ProveedorRepository proveedorRepository;
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
    private Producto producto;

    @DynamicPropertySource
    static void configureTemporarySqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMPORARY_DATABASE);
    }

    @BeforeEach
    void prepararDatos() {
        sesionActual.cerrarSesion();
        ventaRepository.deleteAll();
        movimientoRepository.deleteAll();
        compraRepository.deleteAll();
        loteRepository.deleteAll();
        proveedorRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        laboratorioRepository.deleteAll();
        usuarioRepository.deleteAll();
        owner = usuario("owner", Rol.OWNER);
        seller = usuario("vendedor", Rol.SELLER);

        Categoria categoria = new Categoria();
        categoria.setNombre("Cat");
        categoria = categoriaRepository.saveAndFlush(categoria);
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setNombre("Lab");
        laboratorio = laboratorioRepository.saveAndFlush(laboratorio);
        producto = new Producto();
        producto.setCodigoInterno("P-001");
        producto.setNombreComercial("Paracetamol 500 mg");
        producto.setTipoProducto(TipoProducto.MEDICAMENTO);
        producto.setPrecioCompra(new BigDecimal("5.00"));
        producto.setPrecioVenta(new BigDecimal("11.80"));
        producto.setStockMinimo(5);
        producto.setCategoria(categoria);
        producto.setLaboratorio(laboratorio);
        producto = productoRepository.saveAndFlush(producto);

        sesionActual.autenticar(owner);
        compraService.registrar(new NuevaCompra(
            "20123456789", TipoDocumentoCompra.FACTURA, LocalDate.now(), false, false,
            null, List.of(new LineaCompra(producto.getId(), 10, new BigDecimal("5.00"), "LOTE-A", LocalDate.now().plusDays(20)))
        ));
    }

    @Test
    void resumenCalculaTotalesTicketUtilidadYMasVendidos() {
        vender(3);
        vender(1);

        ResumenVentas resumen = dashboardService.resumenVentas(Periodo.HOY);

        assertDecimal("47.20", resumen.total());
        assertEquals(2, resumen.cantidadVentas());
        assertDecimal("23.60", resumen.ticketPromedio());
        // ingreso sin IGV 40.00 - costo 4 x 5.00 = 20.00
        assertDecimal("20.00", resumen.utilidadBruta());
        assertEquals(1, resumen.masVendidos().size());
        assertEquals(4L, resumen.masVendidos().get(0).unidades());
        assertNull(resumen.variacionPorcentual());
        assertEquals(1, resumen.serie().size());
        assertDecimal("47.20", resumen.serie().get(0).total());
    }

    @Test
    void ventasAnuladasNoCuentan() {
        VentaResumen venta = vender(2);
        vender(1);
        ventaService.anular(venta.id());

        ResumenVentas resumen = dashboardService.resumenVentas(Periodo.HOY);

        assertEquals(1, resumen.cantidadVentas());
        assertDecimal("11.80", resumen.total());
    }

    @Test
    void periodoLargoAgrupaPorMesYRellenaConCeros() {
        vender(1);

        ResumenVentas anio = dashboardService.resumenVentas(Periodo.ESTE_ANIO, LocalDate.now());
        ResumenVentas semana = dashboardService.resumenVentas(Periodo.ULTIMOS_7);

        assertEquals(LocalDate.now().getMonthValue(), anio.serie().size());
        assertEquals(7, semana.serie().size());
        assertDecimal("11.80", semana.serie().get(6).total());
        assertDecimal("0", semana.serie().get(0).total());
    }

    @Test
    void sinVentasTodoQuedaEnCero() {
        ResumenVentas resumen = dashboardService.resumenVentas(Periodo.ESTE_MES);

        assertEquals(0, resumen.cantidadVentas());
        assertDecimal("0", resumen.total());
        assertDecimal("0", resumen.ticketPromedio());
        assertTrue(resumen.masVendidos().isEmpty());
    }

    @Test
    void sellerVeAlertasPeroNoLosIndicadoresDeVentas() {
        sesionActual.autenticar(seller);

        assertThrows(NegocioException.class, () -> dashboardService.resumenVentas(Periodo.HOY));
        Alertas alertas = dashboardService.alertas();
        // 10 unidades con minimo 5: disponible, y el lote vence en 20 dias
        assertEquals(0, alertas.productosConStockBajo());
        assertEquals(1, alertas.lotesPorVencer());
        assertEquals(0, alertas.lotesVencidos());
    }

    @Test
    void alertasDetectanStockBajoYSinStock() {
        vender(5);
        assertEquals(1, dashboardService.alertas().productosConStockBajo());
        vender(5);

        Alertas alertas = dashboardService.alertas();

        assertEquals(0, alertas.productosConStockBajo());
        assertEquals(1, alertas.productosSinStock());
        assertEquals(1, alertas.stock().size());
    }

    // ---- utilidades ----

    private VentaResumen vender(int cantidad) {
        return ventaService.registrar(new NuevaVenta(
            TipoComprobante.BOLETA, null, null, true, true, null, List.of(new LineaVenta(producto.getId(), cantidad, null))
        ));
    }

    private static void assertDecimal(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    private Usuario usuario(String username, Rol rol) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword("$2a$10$no-se-usa-en-estas-pruebas");
        u.setRoles(rol);
        u.setEstado(true);
        return usuarioRepository.saveAndFlush(u);
    }

    private static Path createTemporaryDatabasePath() {
        try {
            return Files.createTempDirectory("botica-dashboard-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
