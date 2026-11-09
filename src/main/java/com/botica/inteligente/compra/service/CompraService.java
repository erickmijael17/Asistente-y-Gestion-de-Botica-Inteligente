package com.botica.inteligente.compra.service;

import com.botica.inteligente.compra.entity.Compra;
import com.botica.inteligente.compra.entity.CompraDetalle;
import com.botica.inteligente.compra.enums.EstadoCompra;
import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import com.botica.inteligente.compra.repository.CompraRepository;
import com.botica.inteligente.compra.specification.CompraSpecification;
import com.botica.inteligente.inventario.entity.Lote;
import com.botica.inteligente.inventario.enums.TipoMovimiento;
import com.botica.inteligente.inventario.service.InventarioService;
import com.botica.inteligente.inventario.service.MovimientoContexto;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.enums.TipoProducto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.proveedor.entity.Proveedor;
import com.botica.inteligente.proveedor.repository.ProveedorRepository;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import com.botica.inteligente.shared.util.CalculadoraIgv;
import com.botica.inteligente.shared.util.Textos;
import com.botica.inteligente.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraService {

    private static final Logger log = LoggerFactory.getLogger(CompraService.class);
    private static final Pattern RUC = Pattern.compile("(10|15|17|20)\\d{9}");

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final SesionActual sesionActual;
    private final BigDecimal tasaIgv;

    public CompraService(
        CompraRepository compraRepository,
        ProveedorRepository proveedorRepository,
        ProductoRepository productoRepository,
        UsuarioRepository usuarioRepository,
        InventarioService inventarioService,
        SesionActual sesionActual,
        @Value("${botica.igv.tasa:0.18}") BigDecimal tasaIgv
    ) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.sesionActual = sesionActual;
        this.tasaIgv = tasaIgv;
    }

    @Transactional(readOnly = true)
    public Page<CompraResumen> listar(
        String texto, EstadoCompra estado, LocalDate desde, LocalDate hasta, Pageable pageable
    ) {
        sesionActual.exigirAutenticado();
        return compraRepository.findAll(CompraSpecification.filtrar(texto, estado, desde, hasta), pageable)
            .map(compra -> resumen(compra, false));
    }

    @Transactional(readOnly = true)
    public CompraResumen obtener(Long id) {
        sesionActual.exigirAutenticado();
        return resumen(buscarConDetalle(id), true);
    }

    @Transactional
    public CompraResumen registrar(NuevaCompra nueva) {
        sesionActual.exigirOwner(Mensajes.get("error.compra.sinPermiso"));
        String ruc = Textos.opcional("ruc", "el RUC del proveedor", nueva.ruc(), 11);
        if (ruc != null && !RUC.matcher(ruc).matches()) {
            throw new CampoInvalidoException("ruc", Mensajes.get("error.compra.rucInvalido"));
        }
        TipoDocumentoCompra tipoDocumento = nueva.tipoDocumento();
        if (tipoDocumento == null) {
            throw new CampoInvalidoException("tipoDocumento", Mensajes.get("error.compra.tipoDocumentoRequerido"));
        }
        LocalDate hoy = LocalDate.now();
        if (nueva.fechaDocumento() == null) {
            throw new CampoInvalidoException("fechaDocumento", Mensajes.get("error.compra.fechaRequerida"));
        }
        if (nueva.fechaDocumento().isAfter(hoy)) {
            throw new CampoInvalidoException("fechaDocumento", Mensajes.get("error.compra.fechaFutura"));
        }
        if (nueva.lineas() == null || nueva.lineas().isEmpty()) {
            throw new CampoInvalidoException("lineas", Mensajes.get("error.compra.sinLineas"));
        }

        Proveedor proveedor = ruc == null ? null : proveedorRepository.findByRuc(ruc).orElseGet(() -> {
            Proveedor creado = new Proveedor();
            creado.setRuc(ruc);
            creado.setRazonSocial(ruc);
            return proveedorRepository.save(creado);
        });

        int numero = compraRepository.ultimoNumero() + 1;
        Compra compra = new Compra();
        compra.setNumero(numero);
        compra.setCodigo(Compra.formatearCodigo(numero));
        compra.setFecha(LocalDateTime.now());
        compra.setProveedor(proveedor);
        compra.setUsuario(usuarioRepository.getReferenceById(sesionActual.usuarioAutenticado().orElseThrow().id()));
        compra.setTipoDocumento(tipoDocumento);
        compra.setFechaDocumento(nueva.fechaDocumento());
        compra.setAplicaIgv(nueva.aplicaIgv());
        compra.setPreciosIncluyenIgv(nueva.preciosIncluyenIgv());
        compra.setObservacion(Textos.opcional("observacion", "observación", nueva.observacion(), 500));
        compra.setEstado(EstadoCompra.REGISTRADA);

        List<BigDecimal> importes = new ArrayList<>();
        List<LineaValidada> validadas = new ArrayList<>();
        for (int i = 0; i < nueva.lineas().size(); i++) {
            LineaValidada linea = validarLinea(i + 1, nueva.lineas().get(i), compra.getCodigo(), hoy);
            validadas.add(linea);
            importes.add(linea.importe());
        }
        CalculadoraIgv.Totales totales = CalculadoraIgv.calcular(
            importes, nueva.aplicaIgv(), nueva.preciosIncluyenIgv(), tasaIgv
        );
        compra.setTasaIgv(totales.tasa());
        compra.setBaseImponible(totales.baseImponible());
        compra.setIgv(totales.igv());
        compra.setTotal(totales.total());
        Compra guardada = compraRepository.save(compra);

        MovimientoContexto contexto = new MovimientoContexto(
            guardada.getCodigo(), guardada.getId(), null,
            sesionActual.usuarioAutenticado().orElseThrow().id(), guardada.getFecha()
        );
        for (LineaValidada linea : validadas) {
            BigDecimal costoSinIgv = CalculadoraIgv.sinIgv(
                linea.linea().costoUnitario(), nueva.aplicaIgv(), nueva.preciosIncluyenIgv(), tasaIgv
            );
            Lote lote = inventarioService.registrarEntrada(
                linea.producto(), linea.codigoLote(), linea.linea().fechaVencimiento(),
                linea.linea().cantidad(), costoSinIgv, TipoMovimiento.COMPRA, contexto
            );
            CompraDetalle detalle = new CompraDetalle();
            detalle.setProducto(linea.producto());
            detalle.setLote(lote);
            detalle.setCodigoProducto(linea.producto().getCodigoInterno());
            detalle.setNombreProducto(linea.producto().getNombreComercial());
            detalle.setCantidad(linea.linea().cantidad());
            detalle.setCostoUnitario(linea.linea().costoUnitario().setScale(4, java.math.RoundingMode.HALF_UP));
            detalle.setImporte(linea.importe());
            guardada.agregarDetalle(detalle);
        }
        Compra completa = compraRepository.save(guardada);
        log.info("Compra {} registrada con {} líneas", completa.getCodigo(), validadas.size());
        return resumen(completa, true);
    }

    @Transactional
    public CompraResumen anular(Long id) {
        sesionActual.exigirOwner(Mensajes.get("error.compra.sinPermiso"));
        Compra compra = buscarConDetalle(id);
        if (compra.getEstado() == EstadoCompra.ANULADA) {
            throw new NegocioException(Mensajes.get("error.compra.yaAnulada"));
        }
        for (CompraDetalle detalle : compra.getDetalles()) {
            if (detalle.getLote().getCantidadActual() < detalle.getCantidad()) {
                throw new NegocioException(
                    Mensajes.get("error.compra.loteYaVendido", detalle.getLote().getCodigoLote())
                );
            }
        }
        MovimientoContexto contexto = new MovimientoContexto(
            compra.getCodigo(), compra.getId(), null,
            sesionActual.usuarioAutenticado().orElseThrow().id(), LocalDateTime.now()
        );
        for (CompraDetalle detalle : compra.getDetalles()) {
            inventarioService.registrarSalidaDeLote(
                detalle.getLote(), detalle.getCantidad(), TipoMovimiento.ANULACION_COMPRA, contexto
            );
        }
        compra.setEstado(EstadoCompra.ANULADA);
        log.info("Compra {} anulada", compra.getCodigo());
        return resumen(compraRepository.save(compra), true);
    }

    private record LineaValidada(LineaCompra linea, Producto producto, String codigoLote, BigDecimal importe) {
    }

    private LineaValidada validarLinea(int item, LineaCompra linea, String codigoCompra, LocalDate hoy) {
        String campo = "lineas";
        String numero = String.valueOf(item);
        if (linea.productoId() == null) {
            throw new CampoInvalidoException(campo, Mensajes.get("error.compra.linea.seleccioneProducto", numero));
        }
        Producto producto = productoRepository.findById(linea.productoId())
            .orElseThrow(() -> new CampoInvalidoException(campo, Mensajes.get("error.compra.linea.productoNoExiste", numero)));
        if (!Boolean.TRUE.equals(producto.getEstado())) {
            throw new CampoInvalidoException(campo, Mensajes.get("error.compra.linea.productoInactivo", numero));
        }
        if (linea.cantidad() < 1) {
            throw new CampoInvalidoException(campo, Mensajes.get("error.compra.linea.cantidadMinima", numero));
        }
        if (linea.costoUnitario() == null || linea.costoUnitario().signum() <= 0) {
            throw new CampoInvalidoException(campo, Mensajes.get("error.compra.linea.costoMayorACero", numero));
        }
        LocalDate vencimiento = linea.fechaVencimiento();
        if (vencimiento == null && producto.getTipoProducto() != TipoProducto.OTRO) {
            throw new CampoInvalidoException(campo, Mensajes.get("error.compra.linea.vencimientoRequerido", numero));
        }
        if (vencimiento != null && !vencimiento.isAfter(hoy)) {
            throw new CampoInvalidoException(campo, Mensajes.get("error.compra.linea.productoVencido", numero));
        }
        String codigoLote = Textos.opcional(campo, "lote", linea.codigoLote(), 60);
        if (codigoLote == null) {
            codigoLote = codigoCompra + "-" + item;
        }
        return new LineaValidada(
            linea, producto, codigoLote, CalculadoraIgv.importe(linea.cantidad(), linea.costoUnitario())
        );
    }

    private Compra buscarConDetalle(Long id) {
        return compraRepository.buscarConDetalle(id)
            .orElseThrow(() -> new NegocioException(Mensajes.get("error.compra.noEncontrada")));
    }

    private CompraResumen resumen(Compra c, boolean conDetalle) {
        List<CompraResumen.Detalle> detalles = conDetalle
            ? c.getDetalles().stream().map(d -> new CompraResumen.Detalle(
                d.getCodigoProducto(), d.getNombreProducto(), d.getCantidad(), d.getCostoUnitario(),
                d.getImporte(), d.getLote().getCodigoLote(), d.getLote().getFechaVencimiento()
            )).toList()
            : List.of();
        return new CompraResumen(
            c.getId(), c.getCodigo(), c.getFecha(),
            c.getProveedor() == null ? null : c.getProveedor().getRazonSocial(),
            c.getProveedor() == null ? null : c.getProveedor().getRuc(),
            c.getTipoDocumento(), c.getFechaDocumento(), c.getBaseImponible(),
            c.getIgv(), c.getTotal(), c.getEstado(), detalles
        );
    }
}
