package com.botica.inteligente.venta.service;

import com.botica.inteligente.inventario.entity.MovimientoInventario;
import com.botica.inteligente.inventario.enums.TipoMovimiento;
import com.botica.inteligente.inventario.repository.MovimientoInventarioRepository;
import com.botica.inteligente.inventario.service.InventarioService;
import com.botica.inteligente.inventario.service.MovimientoContexto;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import com.botica.inteligente.shared.util.CalculadoraIgv;
import com.botica.inteligente.shared.util.Textos;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import com.botica.inteligente.venta.entity.Venta;
import com.botica.inteligente.venta.entity.VentaDetalle;
import com.botica.inteligente.venta.enums.EstadoVenta;
import com.botica.inteligente.venta.enums.TipoComprobante;
import com.botica.inteligente.venta.repository.VentaRepository;
import com.botica.inteligente.venta.specification.VentaSpecification;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);
    private static final int CANTIDAD_MAXIMA = 9999;
    private static final Pattern DOCUMENTO_CLIENTE = Pattern.compile("\\d{8}|\\d{11}");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final InventarioService inventarioService;
    private final SesionActual sesionActual;
    private final BigDecimal tasaIgv;
    private final String serieBoleta;
    private final String serieNotaVenta;

    public VentaService(
        VentaRepository ventaRepository,
        ProductoRepository productoRepository,
        UsuarioRepository usuarioRepository,
        MovimientoInventarioRepository movimientoRepository,
        InventarioService inventarioService,
        SesionActual sesionActual,
        @Value("${botica.igv.tasa:0.18}") BigDecimal tasaIgv,
        @Value("${botica.serie.boleta:B001}") String serieBoleta,
        @Value("${botica.serie.nota-venta:NV01}") String serieNotaVenta
    ) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
        this.inventarioService = inventarioService;
        this.sesionActual = sesionActual;
        this.tasaIgv = tasaIgv;
        this.serieBoleta = serieBoleta;
        this.serieNotaVenta = serieNotaVenta;
    }

    @Transactional(readOnly = true)
    public Page<VentaResumen> listar(
        String texto, EstadoVenta estado, LocalDate desde, LocalDate hasta, Pageable pageable
    ) {
        sesionActual.exigirAutenticado();
        return ventaRepository.findAll(VentaSpecification.filtrar(texto, estado, desde, hasta), pageable)
            .map(venta -> resumen(venta, false));
    }

    @Transactional(readOnly = true)
    public VentaResumen obtener(Long id) {
        sesionActual.exigirAutenticado();
        return resumen(buscarConDetalle(id), true);
    }

    /** Total vendido (solo ventas emitidas) entre dos fechas, ambas inclusive. */
    @Transactional(readOnly = true)
    public BigDecimal totalEmitido(LocalDate desde, LocalDate hasta) {
        sesionActual.exigirAutenticado();
        return ventaRepository.totalEmitido(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
    }

    @Transactional
    public VentaResumen registrar(NuevaVenta nueva) {
        sesionActual.exigirAutenticado();
        TipoComprobante tipo = nueva.tipoComprobante();
        if (tipo == null) {
            throw new CampoInvalidoException("tipoComprobante", Mensajes.get("error.venta.tipoComprobanteRequerido"));
        }
        String documento = Textos.opcional("clienteDocumento", "documento del cliente", nueva.clienteDocumento(), 15);
        if (documento != null && !DOCUMENTO_CLIENTE.matcher(documento).matches()) {
            throw new CampoInvalidoException("clienteDocumento", Mensajes.get("error.venta.documentoInvalido"));
        }
        if (nueva.lineas() == null || nueva.lineas().isEmpty()) {
            throw new CampoInvalidoException("lineas", Mensajes.get("error.venta.sinLineas"));
        }

        List<LineaCalculada> lineas = calcularLineas(nueva.lineas());
        Map<Producto, Integer> solicitado = new LinkedHashMap<>();
        lineas.forEach(l -> solicitado.merge(l.producto(), l.linea().cantidad(), Integer::sum));
        inventarioService.validarDisponible(solicitado);

        CalculadoraIgv.Totales totales = CalculadoraIgv.calcular(
            lineas.stream().map(LineaCalculada::importe).toList(),
            nueva.aplicaIgv(), nueva.preciosIncluyenIgv(), tasaIgv
        );

        String serie = tipo == TipoComprobante.BOLETA ? serieBoleta : serieNotaVenta;
        int numero = ventaRepository.ultimoNumero(serie) + 1;
        Long usuarioId = sesionActual.usuarioAutenticado().orElseThrow().id();

        Venta venta = new Venta();
        venta.setTipoComprobante(tipo);
        venta.setSerie(serie);
        venta.setNumero(numero);
        venta.setComprobante(Venta.formatearComprobante(serie, numero));
        venta.setFecha(LocalDateTime.now());
        venta.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        venta.setClienteNombre(Textos.opcional("clienteNombre", "nombre del cliente", nueva.clienteNombre(), 150));
        venta.setClienteDocumento(documento);
        venta.setAplicaIgv(nueva.aplicaIgv());
        venta.setPreciosIncluyenIgv(nueva.preciosIncluyenIgv());
        venta.setTasaIgv(totales.tasa());
        venta.setBaseImponible(totales.baseImponible());
        venta.setIgv(totales.igv());
        venta.setTotal(totales.total());
        venta.setEstado(EstadoVenta.EMITIDA);
        venta.setObservacion(Textos.opcional("observacion", "observación", nueva.observacion(), 500));
        Venta guardada = ventaRepository.save(venta);

        MovimientoContexto contexto = new MovimientoContexto(
            guardada.getComprobante(), null, guardada.getId(), usuarioId, guardada.getFecha()
        );
        for (LineaCalculada linea : lineas) {
            Producto producto = linea.producto();
            inventarioService.registrarSalidaFefo(producto, linea.linea().cantidad(), TipoMovimiento.VENTA, contexto);
            VentaDetalle detalle = new VentaDetalle();
            detalle.setProducto(producto);
            detalle.setCodigoProducto(producto.getCodigoInterno());
            detalle.setNombreProducto(producto.getNombreComercial());
            detalle.setCantidad(linea.linea().cantidad());
            detalle.setPrecioUnitario(linea.precio());
            detalle.setImporte(linea.importe());
            detalle.setCostoUnitario(producto.getCostoPromedio());
            guardada.agregarDetalle(detalle);
        }
        Venta completa = ventaRepository.save(guardada);
        log.info("Venta {} registrada por {} con {} líneas", completa.getComprobante(), usuarioId, lineas.size());
        return resumen(completa, true);
    }

    @Transactional
    public VentaResumen anular(Long id) {
        sesionActual.exigirOwner(Mensajes.get("error.venta.soloOwnerAnula"));
        Venta venta = buscarConDetalle(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new NegocioException(Mensajes.get("error.venta.yaAnulada"));
        }
        Long usuarioId = sesionActual.usuarioAutenticado().orElseThrow().id();
        MovimientoContexto contexto = new MovimientoContexto(
            venta.getComprobante(), null, venta.getId(), usuarioId, LocalDateTime.now()
        );
        for (MovimientoInventario salida : movimientoRepository.deVenta(venta.getId(), TipoMovimiento.VENTA)) {
            inventarioService.registrarEntrada(
                salida.getProducto(), salida.getLote().getCodigoLote(), salida.getLote().getFechaVencimiento(),
                salida.getCantidad(), salida.getLote().getCostoUnitario(), TipoMovimiento.ANULACION_VENTA, contexto
            );
        }
        venta.setEstado(EstadoVenta.ANULADA);
        log.info("Venta {} anulada", venta.getComprobante());
        return resumen(ventaRepository.save(venta), true);
    }

    private record LineaCalculada(LineaVenta linea, Producto producto, BigDecimal precio, BigDecimal importe) {
    }

    private List<LineaCalculada> calcularLineas(List<LineaVenta> lineas) {
        List<Long> ids = lineas.stream().map(LineaVenta::productoId).filter(java.util.Objects::nonNull).toList();
        Map<Long, Producto> productos = productoRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(Producto::getId, Function.identity()));
        List<LineaCalculada> calculadas = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            LineaVenta linea = lineas.get(i);
            int item = i + 1;
            Producto producto = linea.productoId() == null ? null : productos.get(linea.productoId());
            if (producto == null) {
                throw new CampoInvalidoException("lineas", Mensajes.get("error.venta.linea.productoNoExiste", String.valueOf(item)));
            }
            if (!Boolean.TRUE.equals(producto.getEstado())) {
                throw new CampoInvalidoException(
                    "lineas",
                    Mensajes.get("error.venta.linea.productoInactivo", String.valueOf(item), producto.getNombreComercial())
                );
            }
            if (linea.cantidad() < 1 || linea.cantidad() > CANTIDAD_MAXIMA) {
                throw new CampoInvalidoException(
                    "lineas",
                    Mensajes.get("error.venta.linea.cantidadRango", String.valueOf(item), String.valueOf(CANTIDAD_MAXIMA))
                );
            }
            BigDecimal precio = precioDeLinea(item, linea, producto);
            calculadas.add(new LineaCalculada(linea, producto, precio, CalculadoraIgv.importe(linea.cantidad(), precio)));
        }
        return calculadas;
    }

    /** El vendedor usa el precio del catalogo; solo el propietario puede cambiarlo. */
    private BigDecimal precioDeLinea(int item, LineaVenta linea, Producto producto) {
        BigDecimal catalogo = producto.getPrecioVenta();
        BigDecimal pedido = linea.precioUnitario();
        if (pedido == null || pedido.compareTo(catalogo) == 0) {
            return catalogo;
        }
        if (pedido.signum() <= 0) {
            throw new CampoInvalidoException("lineas", Mensajes.get("error.venta.linea.precioMayorACero", String.valueOf(item)));
        }
        sesionActual.exigirOwner(Mensajes.get("error.venta.soloOwnerCambiaPrecio"));
        return pedido.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private Venta buscarConDetalle(Long id) {
        return ventaRepository.buscarConDetalle(id)
            .orElseThrow(() -> new NegocioException(Mensajes.get("error.venta.noEncontrada")));
    }

    private VentaResumen resumen(Venta v, boolean conDetalle) {
        List<VentaResumen.Detalle> detalles = conDetalle
            ? v.getDetalles().stream().map(d -> new VentaResumen.Detalle(
                d.getCodigoProducto(), d.getNombreProducto(), d.getCantidad(), d.getPrecioUnitario(), d.getImporte()
            )).toList()
            : List.of();
        return new VentaResumen(
            v.getId(), v.getComprobante(), v.getTipoComprobante(), v.getFecha(), v.getUsuario().getUsername(),
            v.getClienteNombre(), v.getClienteDocumento(), Boolean.TRUE.equals(v.getAplicaIgv()),
            Boolean.TRUE.equals(v.getPreciosIncluyenIgv()), v.getBaseImponible(), v.getIgv(), v.getTotal(),
            v.getEstado(), v.getObservacion(), detalles
        );
    }
}
