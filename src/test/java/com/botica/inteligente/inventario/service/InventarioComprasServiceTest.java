package com.botica.inteligente.inventario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import com.botica.inteligente.compra.repository.CompraRepository;
import com.botica.inteligente.compra.service.CompraResumen;
import com.botica.inteligente.compra.service.CompraService;
import com.botica.inteligente.compra.service.LineaCompra;
import com.botica.inteligente.compra.service.NuevaCompra;
import com.botica.inteligente.inventario.enums.TipoMovimiento;
import com.botica.inteligente.inventario.repository.LoteRepository;
import com.botica.inteligente.inventario.repository.MovimientoInventarioRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class InventarioComprasServiceTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();
    private static final String RUC = "20123456789";

    @Autowired
    private CompraService compraService;
    @Autowired
    private InventarioService inventarioService;
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private LoteRepository loteRepository;
    @Autowired
    private MovimientoInventarioRepository movimientoRepository;
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
    @Autowired
    private TransactionTemplate transaccion;

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
        sesionActual.autenticar(owner);

        Categoria categoria = new Categoria();
        categoria.setNombre("Cat");
        categoria = categoriaRepository.saveAndFlush(categoria);
        Laboratorio laboratorio = new Laboratorio();
        laboratorio.setNombre("Lab");
        laboratorio = laboratorioRepository.saveAndFlush(laboratorio);
        producto = producto("P-001", categoria, laboratorio);
    }

    @Test
    void compraCreaLoteStockKardexYCostoPromedio() {
        compraService.registrar(compra("F001-1", linea(10, "5.00", "LOTE-A", 400)));

        assertEquals(10, inventarioService.stockTotal(producto.getId()));
        assertDecimal("5.0000", productoRepository.findById(producto.getId()).orElseThrow().getCostoPromedio());

        compraService.registrar(compra("F001-2", linea(10, "7.00", "LOTE-B", 500)));

        assertEquals(20, inventarioService.stockTotal(producto.getId()));
        assertDecimal("6.0000", productoRepository.findById(producto.getId()).orElseThrow().getCostoPromedio());
        Page<KardexFila> kardex = inventarioService.kardex(producto.getId(), 0, 20);
        assertEquals(2, kardex.getTotalElements());
        assertEquals(20, kardex.getContent().get(0).saldo());
    }

    @Test
    void compraConIgvIncluidoCostealSinImpuesto() {
        compraService.registrar(new NuevaCompra(
            RUC, TipoDocumentoCompra.FACTURA, LocalDate.now(), true, true, null,
            List.of(linea(10, "11.80", "LOTE-A", 400))
        ));

        assertDecimal("10.0000", productoRepository.findById(producto.getId()).orElseThrow().getCostoPromedio());
    }

    @Test
    void mismoLoteSumaCantidadYRechazaOtroVencimiento() {
        compraService.registrar(compra("F001-1", linea(5, "5.00", "LOTE-A", 400)));
        compraService.registrar(compra("F001-2", linea(5, "5.00", "LOTE-A", 400)));

        assertEquals(1, loteRepository.deProducto(producto.getId()).size());
        assertEquals(10, inventarioService.stockTotal(producto.getId()));
        CampoInvalidoException error = assertThrows(
            CampoInvalidoException.class,
            () -> compraService.registrar(compra("F001-3", linea(5, "5.00", "LOTE-A", 800)))
        );
        assertEquals("lote", error.getCampo());
    }

    @Test
    void compraValidaRucFechaVencimientoYLineas() {
        assertEquals("ruc", assertThrows(CampoInvalidoException.class, () -> compraService.registrar(
            new NuevaCompra("123", TipoDocumentoCompra.FACTURA, LocalDate.now(), false, false, null,
                List.of(linea(1, "5.00", "L", 400))))).getCampo());
        assertEquals("fechaDocumento", assertThrows(CampoInvalidoException.class, () -> compraService.registrar(
            new NuevaCompra(RUC, TipoDocumentoCompra.FACTURA, LocalDate.now().plusDays(1), false, false,
                null, List.of(linea(1, "5.00", "L", 400))))).getCampo());
        assertEquals("lineas", assertThrows(CampoInvalidoException.class,
            () -> compraService.registrar(compra("F001-5", linea(1, "5.00", "L", -1)))).getCampo());
        assertEquals("lineas", assertThrows(CampoInvalidoException.class,
            () -> compraService.registrar(compra("F001-6", linea(0, "5.00", "L", 400)))).getCampo());
        assertEquals("lineas", assertThrows(CampoInvalidoException.class, () -> compraService.registrar(
            new NuevaCompra(RUC, TipoDocumentoCompra.FACTURA, LocalDate.now(), false, false, null,
                List.of()))).getCampo());
    }

    @Test
    void compraSinRucSeRegistraSinProveedor() {
        CompraResumen resultado = compraService.registrar(new NuevaCompra(
            null, TipoDocumentoCompra.FACTURA, LocalDate.now(), false, false, null,
            List.of(linea(5, "5.00", "LOTE-A", 400))
        ));

        assertEquals(null, resultado.ruc());
        assertEquals(null, resultado.proveedor());
    }

    @Test
    void sellerNoPuedeRegistrarNiAnularCompras() {
        CompraResumen compra = compraService.registrar(compra("F001-1", linea(5, "5.00", "LOTE-A", 400)));
        sesionActual.autenticar(seller);

        assertThrows(NegocioException.class,
            () -> compraService.registrar(compra("F001-2", linea(1, "5.00", "LOTE-B", 400))));
        assertThrows(NegocioException.class, () -> compraService.anular(compra.id()));
        assertEquals(1, compraService.listar(null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10))
            .getTotalElements());
    }

    @Test
    void anularCompraRevierteStockYNoSePuedeAnularDosVeces() {
        CompraResumen compra = compraService.registrar(compra("F001-1", linea(10, "5.00", "LOTE-A", 400)));

        CompraResumen anulada = compraService.anular(compra.id());

        assertEquals(0, inventarioService.stockTotal(producto.getId()));
        assertEquals(com.botica.inteligente.compra.enums.EstadoCompra.ANULADA, anulada.estado());
        assertThrows(NegocioException.class, () -> compraService.anular(compra.id()));
        Page<KardexFila> kardex = inventarioService.kardex(producto.getId(), 0, 20);
        assertEquals(TipoMovimiento.ANULACION_COMPRA, kardex.getContent().get(0).tipo());
        assertEquals(0, kardex.getContent().get(0).saldo());
        // el documento anulado puede volver a registrarse
        compraService.registrar(compra("F001-1", linea(2, "5.00", "LOTE-C", 400)));
    }

    @Test
    void noSePuedeAnularUnaCompraCuyaMercaderiaYaSeVendio() {
        CompraResumen compra = compraService.registrar(compra("F001-1", linea(10, "5.00", "LOTE-A", 400)));
        transaccion.executeWithoutResult(estado -> inventarioService.registrarSalidaFefo(
            productoRepository.findById(producto.getId()).orElseThrow(), 3, TipoMovimiento.VENTA,
            MovimientoContexto.ajuste("V-1", owner.getId())));

        NegocioException error = assertThrows(NegocioException.class, () -> compraService.anular(compra.id()));

        assertTrue(error.getMessage().contains("ya se vendió"));
        assertEquals(7, inventarioService.stockTotal(producto.getId()));
    }

    @Test
    void salidaFefoConsumeElLoteQueVenceAntesEIgnoraVencidos() {
        compraService.registrar(compra("F001-1", linea(5, "5.00", "LOTE-LEJANO", 900)));
        compraService.registrar(compra("F001-2", linea(5, "5.00", "LOTE-CERCANO", 100)));
        // lote vencido directo en BD (una compra no permite vencidos)
        transaccion.executeWithoutResult(estado -> inventarioService.registrarEntrada(
            productoRepository.findById(producto.getId()).orElseThrow(), "LOTE-VENCIDO",
            LocalDate.now().minusDays(5), 50, new BigDecimal("1.00"), TipoMovimiento.AJUSTE_ENTRADA,
            MovimientoContexto.ajuste("AJ-1", owner.getId())));

        assertEquals(10, inventarioService.stockDisponible(producto.getId()));
        assertEquals(60, inventarioService.stockTotal(producto.getId()));

        List<Integer> cantidadesPorLote = transaccion.execute(estado ->
            inventarioService.registrarSalidaFefo(
                productoRepository.findById(producto.getId()).orElseThrow(), 7, TipoMovimiento.VENTA,
                MovimientoContexto.ajuste("V-1", owner.getId())).stream()
                .map(InventarioService.ConsumoLote::cantidad).toList());
        assertEquals(List.of(5, 2), cantidadesPorLote);

        assertEquals(0, loteRepository.findByProductoIdAndCodigoLote(producto.getId(), "LOTE-CERCANO")
            .orElseThrow().getCantidadActual());
        assertEquals(3, loteRepository.findByProductoIdAndCodigoLote(producto.getId(), "LOTE-LEJANO")
            .orElseThrow().getCantidadActual());
        assertEquals(50, loteRepository.findByProductoIdAndCodigoLote(producto.getId(), "LOTE-VENCIDO")
            .orElseThrow().getCantidadActual());
    }

    @Test
    void salidaConStockInsuficienteNoCambiaNada() {
        compraService.registrar(compra("F001-1", linea(5, "5.00", "LOTE-A", 400)));

        NegocioException error = assertThrows(NegocioException.class, () -> transaccion.executeWithoutResult(
            estado -> inventarioService.registrarSalidaFefo(
                productoRepository.findById(producto.getId()).orElseThrow(), 6, TipoMovimiento.VENTA,
                MovimientoContexto.ajuste("V-1", owner.getId()))));

        assertTrue(error.getMessage().contains("Stock insuficiente"));
        assertEquals(5, inventarioService.stockTotal(producto.getId()));
    }

    @Test
    void kardexCuadraConElStockDespuesDeVariosMovimientos() {
        compraService.registrar(compra("F001-1", linea(10, "5.00", "LOTE-A", 400)));
        transaccion.executeWithoutResult(estado -> inventarioService.registrarSalidaFefo(
            productoRepository.findById(producto.getId()).orElseThrow(), 4, TipoMovimiento.VENTA,
            MovimientoContexto.ajuste("V-1", owner.getId())));
        compraService.registrar(compra("F001-2", linea(3, "5.00", "LOTE-B", 500)));

        Page<KardexFila> kardex = inventarioService.kardex(producto.getId(), 0, 50);

        assertEquals(3, kardex.getTotalElements());
        assertEquals(inventarioService.stockTotal(producto.getId()), kardex.getContent().get(0).saldo());
        assertEquals(9, kardex.getContent().get(0).saldo());
    }

    @Test
    void alertasDeStockMinimoYVencimiento() {
        compraService.registrar(compra("F001-1", linea(5, "5.00", "LOTE-A", 20)));

        // stock minimo = 5 -> con 5 unidades esta en nivel bajo
        List<InventarioFila> bajo = inventarioService.alertasStockMinimo();
        assertEquals(1, bajo.size());
        assertEquals(InventarioFila.EstadoStock.BAJO, bajo.get(0).estadoStock());

        assertEquals(1, inventarioService.alertasVencimiento(30).size());
        assertTrue(inventarioService.alertasVencimiento(10).isEmpty());

        ResumenInventario resumen = inventarioService.resumen();
        assertEquals(1, resumen.productos());
        assertEquals(5, resumen.unidades());
        assertEquals(1, resumen.conStockBajo());
        assertFalse(inventarioService.listar("zzz", InventarioService.FiltroStock.TODOS,
            org.springframework.data.domain.PageRequest.of(0, 10)).hasContent());
    }

    @Test
    void mermaSoloOwnerYDescuentaDelLote() {
        compraService.registrar(compra("F001-1", linea(10, "5.00", "LOTE-A", 400)));
        Long loteId = loteRepository.deProducto(producto.getId()).get(0).getId();
        sesionActual.autenticar(seller);

        assertThrows(NegocioException.class, () -> inventarioService.registrarMerma(loteId, 2, "Dañado"));

        sesionActual.autenticar(owner);
        inventarioService.registrarMerma(loteId, 2, "Dañado");
        assertEquals(8, inventarioService.stockTotal(producto.getId()));
        assertThrows(NegocioException.class, () -> inventarioService.registrarMerma(loteId, 99, "Dañado"));
    }

    // ---- utilidades ----

    private static void assertDecimal(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), "esperado " + esperado + " pero fue " + real);
    }

    private NuevaCompra compra(String documentoIgnorado, LineaCompra linea) {
        return new NuevaCompra(
            RUC, TipoDocumentoCompra.FACTURA, LocalDate.now(), false, false, null,
            List.of(linea)
        );
    }

    private LineaCompra linea(int cantidad, String costo, String lote, int diasParaVencer) {
        return new LineaCompra(
            producto.getId(), cantidad, new BigDecimal(costo), lote, LocalDate.now().plusDays(diasParaVencer)
        );
    }

    private Producto producto(String codigo, Categoria categoria, Laboratorio laboratorio) {
        Producto p = new Producto();
        p.setCodigoInterno(codigo);
        p.setNombreComercial("Producto " + codigo);
        p.setTipoProducto(TipoProducto.MEDICAMENTO);
        p.setPrecioCompra(new BigDecimal("5.00"));
        p.setPrecioVenta(new BigDecimal("8.00"));
        p.setStockMinimo(5);
        p.setCategoria(categoria);
        p.setLaboratorio(laboratorio);
        return productoRepository.saveAndFlush(p);
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
            return Files.createTempDirectory("botica-inventario-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la base SQLite temporal", exception);
        }
    }
}
