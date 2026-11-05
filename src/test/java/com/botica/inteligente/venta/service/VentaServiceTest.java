package com.botica.inteligente.venta.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.botica.inteligente.inventario.service.InventarioService;
import com.botica.inteligente.inventario.service.KardexFila;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.proveedor.repository.ProveedorRepository;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.usuario.entity.Usuario;
import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import com.botica.inteligente.venta.enums.EstadoVenta;
import com.botica.inteligente.venta.enums.TipoComprobante;
import com.botica.inteligente.venta.repository.VentaRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class VentaServiceTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();

    @Autowired
    private VentaService ventaService;
    @Autowired
    private ComprobantePdfService pdfService;
    @Autowired
    private CompraService compraService;
    @Autowired
    private InventarioService inventarioService;
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
            "20123456789", "Distribuidora SAC", TipoDocumentoCompra.FACTURA, "F001-1", LocalDate.now(), false, false,
            null, List.of(new LineaCompra(producto.getId(), 10, new BigDecimal("5.00"), "LOTE-A", LocalDate.now().plusDays(300)))
        ));
        sesionActual.autenticar(seller);
    }

    @Test
    void ventaDescuentaStockCalculaIgvYAsignaCorrelativo() {
        VentaResumen venta = ventaService.registrar(venta(TipoComprobante.BOLETA, 3, null));

        assertEquals("B001-00000001", venta.comprobante());
        assertEquals(0, new BigDecimal("35.40").compareTo(venta.total()));
        assertEquals(0, new BigDecimal("30.00").compareTo(venta.baseImponible()));
        assertEquals(0, new BigDecimal("5.40").compareTo(venta.igv()));
        assertEquals("vendedor", venta.vendedor());
        assertEquals(7, inventarioService.stockTotal(producto.getId()));
        assertEquals("B001-00000002", ventaService.registrar(venta(TipoComprobante.BOLETA, 1, null)).comprobante());
        assertEquals("NV01-00000001", ventaService.registrar(venta(TipoComprobante.NOTA_VENTA, 1, null)).comprobante());
        assertEquals(5, inventarioService.stockTotal(producto.getId()));
    }

    @Test
    void ventaGuardaCostoPromedioYElKardexQuedaConsistente() {
        VentaResumen venta = ventaService.registrar(venta(TipoComprobante.BOLETA, 4, null));

        var kardex = inventarioService.kardex(producto.getId(), 0, 20).getContent();
        KardexFila ultimo = kardex.get(0);
        assertEquals(4, ultimo.salida());
        assertEquals(6, ultimo.saldo());
        assertEquals(venta.comprobante(), ultimo.documento());
        assertEquals(inventarioService.stockTotal(producto.getId()), ultimo.saldo());
        var detalle = ventaRepository.buscarConDetalle(venta.id()).orElseThrow().getDetalles().get(0);
        assertEquals(0, new BigDecimal("5.00").compareTo(detalle.getCostoUnitario()));
    }

    @Test
    void stockInsuficienteRechazaLaVentaSinCambiarNada() {
        NegocioException error = assertThrows(
            NegocioException.class, () -> ventaService.registrar(venta(TipoComprobante.BOLETA, 11, null))
        );

        assertTrue(error.getMessage().contains("Stock insuficiente"));
        assertEquals(10, inventarioService.stockTotal(producto.getId()));
        assertEquals(0, ventaRepository.count());
    }

    @Test
    void lineasRepetidasSeValidanContraElStockTotal() {
        NuevaVenta nueva = new NuevaVenta(TipoComprobante.BOLETA, null, null, true, true, null, List.of(
            new LineaVenta(producto.getId(), 6, null), new LineaVenta(producto.getId(), 6, null)
        ));

        assertThrows(NegocioException.class, () -> ventaService.registrar(nueva));
        assertEquals(10, inventarioService.stockTotal(producto.getId()));
    }

    @Test
    void sellerNoPuedeCambiarPrecioPeroOwnerSi() {
        assertThrows(NegocioException.class,
            () -> ventaService.registrar(venta(TipoComprobante.BOLETA, 1, new BigDecimal("9.00"))));

        sesionActual.autenticar(owner);
        VentaResumen venta = ventaService.registrar(venta(TipoComprobante.BOLETA, 1, new BigDecimal("9.00")));
        assertEquals(0, new BigDecimal("9.00").compareTo(venta.total()));
    }

    @Test
    void validaClienteLineasYTipo() {
        assertEquals("clienteDocumento", assertThrows(CampoInvalidoException.class, () -> ventaService.registrar(
            new NuevaVenta(TipoComprobante.BOLETA, "Ana", "123", true, true, null,
                List.of(new LineaVenta(producto.getId(), 1, null))))).getCampo());
        assertEquals("lineas", assertThrows(CampoInvalidoException.class, () -> ventaService.registrar(
            new NuevaVenta(TipoComprobante.BOLETA, null, null, true, true, null, List.of()))).getCampo());
        assertEquals("lineas", assertThrows(CampoInvalidoException.class,
            () -> ventaService.registrar(venta(TipoComprobante.BOLETA, 0, null))).getCampo());
        assertEquals("tipoComprobante", assertThrows(CampoInvalidoException.class,
            () -> ventaService.registrar(venta(null, 1, null))).getCampo());
    }

    @Test
    void soloOwnerAnulaYLaAnulacionRevierteElStock() {
        VentaResumen venta = ventaService.registrar(venta(TipoComprobante.BOLETA, 3, null));
        assertThrows(NegocioException.class, () -> ventaService.anular(venta.id()));

        sesionActual.autenticar(owner);
        VentaResumen anulada = ventaService.anular(venta.id());

        assertEquals(EstadoVenta.ANULADA, anulada.estado());
        assertEquals(10, inventarioService.stockTotal(producto.getId()));
        assertThrows(NegocioException.class, () -> ventaService.anular(venta.id()));
        assertEquals(0, ventaService.totalEmitido(LocalDate.now(), LocalDate.now()).signum());
    }

    @Test
    void listaFiltraYTotalizaSoloVentasEmitidas() {
        ventaService.registrar(venta(TipoComprobante.BOLETA, 1, null));
        VentaResumen segunda = ventaService.registrar(venta(TipoComprobante.BOLETA, 2, null));
        sesionActual.autenticar(owner);
        ventaService.anular(segunda.id());

        var todas = ventaService.listar(null, null, null, null, PageRequest.of(0, 10));
        var emitidas = ventaService.listar(null, EstadoVenta.EMITIDA, LocalDate.now(), LocalDate.now(), PageRequest.of(0, 10));

        assertEquals(2, todas.getTotalElements());
        assertEquals(1, emitidas.getTotalElements());
        assertEquals(0, new BigDecimal("11.80").compareTo(ventaService.totalEmitido(LocalDate.now(), LocalDate.now())));
        assertEquals(1, ventaService.listar("b001-00000001", null, null, null, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void generaElComprobanteEnPdf() {
        VentaResumen venta = ventaService.registrar(venta(TipoComprobante.BOLETA, 2, null));

        byte[] pdf = pdfService.generar(venta.id());

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    // ---- utilidades ----

    private NuevaVenta venta(TipoComprobante tipo, int cantidad, BigDecimal precio) {
        return new NuevaVenta(
            tipo, null, null, true, true, null, List.of(new LineaVenta(producto.getId(), cantidad, precio))
        );
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
            return Files.createTempDirectory("botica-ventas-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
