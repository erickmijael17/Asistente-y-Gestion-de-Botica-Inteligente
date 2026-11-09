package com.botica.inteligente.inventario.service;

import com.botica.inteligente.inventario.entity.Lote;
import com.botica.inteligente.inventario.entity.MovimientoInventario;
import com.botica.inteligente.inventario.enums.TipoMovimiento;
import com.botica.inteligente.inventario.repository.InventarioConsultaRepository;
import com.botica.inteligente.inventario.repository.LoteRepository;
import com.botica.inteligente.inventario.repository.MovimientoInventarioRepository;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import com.botica.inteligente.shared.util.Textos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unica via para cambiar el stock: cada entrada o salida actualiza el lote y deja una linea en el
 * kardex con el saldo resultante. Las salidas siguen FEFO (primero vence, primero sale).
 */
@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final LoteRepository loteRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final InventarioConsultaRepository consultaRepository;
    private final SesionActual sesionActual;

    public InventarioService(
        LoteRepository loteRepository,
        MovimientoInventarioRepository movimientoRepository,
        InventarioConsultaRepository consultaRepository,
        SesionActual sesionActual
    ) {
        this.loteRepository = loteRepository;
        this.movimientoRepository = movimientoRepository;
        this.consultaRepository = consultaRepository;
        this.sesionActual = sesionActual;
    }

    // ---------------- consultas ----------------

    public enum FiltroStock {
        TODOS, DISPONIBLE, BAJO, SIN_STOCK
    }

    @Transactional(readOnly = true)
    public Page<InventarioFila> listar(String texto, FiltroStock filtro, Pageable pageable) {
        sesionActual.exigirAutenticado();
        List<InventarioFila> filas = filtrar(texto, filtro);
        int desde = (int) Math.min(pageable.getOffset(), filas.size());
        int hasta = Math.min(desde + pageable.getPageSize(), filas.size());
        return new PageImpl<>(filas.subList(desde, hasta), pageable, filas.size());
    }

    @Transactional(readOnly = true)
    public ResumenInventario resumen() {
        sesionActual.exigirAutenticado();
        List<InventarioFila> filas = consultaRepository.filas("");
        BigDecimal valorCosto = BigDecimal.ZERO;
        BigDecimal valorVenta = BigDecimal.ZERO;
        long unidades = 0;
        int bajo = 0;
        int sinStock = 0;
        for (InventarioFila fila : filas) {
            unidades += fila.stock();
            valorCosto = valorCosto.add(fila.valorCosto());
            valorVenta = valorVenta.add(fila.valorVenta());
            switch (fila.estadoStock()) {
                case BAJO -> bajo++;
                case SIN_STOCK -> sinStock++;
                default -> { }
            }
        }
        return new ResumenInventario(
            filas.size(), unidades, valorCosto.setScale(2, RoundingMode.HALF_UP),
            valorVenta.setScale(2, RoundingMode.HALF_UP), bajo, sinStock
        );
    }

    /** Productos con stock bajo o agotado, para alertas. */
    @Transactional(readOnly = true)
    public List<InventarioFila> alertasStockMinimo() {
        sesionActual.exigirAutenticado();
        return consultaRepository.filas("").stream()
            .filter(fila -> fila.estadoStock() != InventarioFila.EstadoStock.DISPONIBLE)
            .toList();
    }

    /** Lotes con stock que vencen dentro de los proximos dias (incluye los ya vencidos). */
    @Transactional(readOnly = true)
    public List<LoteResumen> alertasVencimiento(int dias) {
        sesionActual.exigirAutenticado();
        LocalDate hoy = LocalDate.now();
        return loteRepository.conStockQueVencenHasta(hoy.plusDays(dias)).stream()
            .map(lote -> resumen(lote, hoy))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LoteResumen> lotesDeProducto(Long productoId) {
        sesionActual.exigirAutenticado();
        LocalDate hoy = LocalDate.now();
        return loteRepository.deProducto(productoId).stream().map(lote -> resumen(lote, hoy)).toList();
    }

    @Transactional(readOnly = true)
    public Page<KardexFila> kardex(Long productoId, int pagina, int tamano) {
        sesionActual.exigirAutenticado();
        Page<MovimientoInventario> movimientos = movimientoRepository.findByProductoId(
            productoId, PageRequest.of(pagina, tamano, Sort.by("fecha", "id").descending())
        );
        return movimientos.map(m -> new KardexFila(
            m.getFecha(), m.getTipo(), m.getLote() == null ? null : m.getLote().getCodigoLote(),
            m.getTipo().esEntrada() ? m.getCantidad() : 0,
            m.getTipo().esEntrada() ? 0 : m.getCantidad(),
            m.getStockResultante(), m.getCostoUnitario(), m.getDocumento()
        ));
    }

    @Transactional(readOnly = true)
    public long stockTotal(Long productoId) {
        return loteRepository.stockTotal(productoId);
    }

    @Transactional(readOnly = true)
    public long stockDisponible(Long productoId) {
        return loteRepository.stockDisponible(productoId, LocalDate.now());
    }

    // ---------------- operaciones de stock (las usan compras y ventas) ----------------

    /**
     * Registra una entrada en el lote indicado (lo crea si no existe). Para compras recalcula el
     * costo promedio ponderado del producto.
     */
    @Transactional
    public Lote registrarEntrada(
        Producto producto, String codigoLote, LocalDate vencimiento, int cantidad,
        BigDecimal costoSinIgv, TipoMovimiento tipo, MovimientoContexto contexto
    ) {
        if (!tipo.esEntrada()) {
            throw new IllegalArgumentException("El tipo " + tipo + " no es una entrada.");
        }
        if (cantidad <= 0) {
            throw new NegocioException(Mensajes.get("error.inventario.cantidadMayorACero"));
        }
        long stockAntes = loteRepository.stockTotal(producto.getId());
        Lote lote = loteRepository.findByProductoIdAndCodigoLote(producto.getId(), codigoLote).orElse(null);
        if (lote == null) {
            lote = new Lote();
            lote.setProducto(producto);
            lote.setCodigoLote(codigoLote);
            lote.setFechaVencimiento(vencimiento);
            lote.setCantidadInicial(cantidad);
            lote.setCantidadActual(cantidad);
            lote.setCostoUnitario(costoSinIgv);
        } else {
            if (vencimiento != null && !vencimiento.equals(lote.getFechaVencimiento())) {
                throw new CampoInvalidoException(
                    "lote", Mensajes.get("error.inventario.loteOtraFechaVencimiento", codigoLote)
                );
            }
            lote.setCantidadActual(lote.getCantidadActual() + cantidad);
            if (tipo == TipoMovimiento.COMPRA) {
                lote.setCantidadInicial(lote.getCantidadInicial() + cantidad);
            }
        }
        Lote guardado = loteRepository.save(lote);

        if (tipo == TipoMovimiento.COMPRA) {
            producto.setCostoPromedio(costoPromedio(producto.getCostoPromedio(), stockAntes, costoSinIgv, cantidad));
        }
        registrarMovimiento(producto, guardado, tipo, cantidad, costoSinIgv, contexto, stockAntes + cantidad);
        return guardado;
    }

    /** Salida de un lote concreto (anulacion de compra, merma). */
    @Transactional
    public void registrarSalidaDeLote(Lote lote, int cantidad, TipoMovimiento tipo, MovimientoContexto contexto) {
        if (tipo.esEntrada()) {
            throw new IllegalArgumentException("El tipo " + tipo + " no es una salida.");
        }
        if (cantidad <= 0) {
            throw new NegocioException(Mensajes.get("error.inventario.cantidadMayorACero"));
        }
        if (lote.getCantidadActual() < cantidad) {
            throw new NegocioException(
                Mensajes.get(
                    "error.inventario.loteUnidadesInsuficientes",
                    lote.getCodigoLote(), String.valueOf(lote.getCantidadActual())
                )
            );
        }
        long stockAntes = loteRepository.stockTotal(lote.getProducto().getId());
        lote.setCantidadActual(lote.getCantidadActual() - cantidad);
        loteRepository.save(lote);
        registrarMovimiento(
            lote.getProducto(), lote, tipo, cantidad, lote.getCostoUnitario(), contexto, stockAntes - cantidad
        );
    }

    /**
     * Salida por FEFO: consume primero los lotes que vencen antes e ignora los vencidos.
     *
     * @return lotes consumidos y cantidad tomada de cada uno
     */
    @Transactional
    public List<ConsumoLote> registrarSalidaFefo(
        Producto producto, int cantidad, TipoMovimiento tipo, MovimientoContexto contexto
    ) {
        if (cantidad <= 0) {
            throw new NegocioException(Mensajes.get("error.inventario.cantidadMayorACero"));
        }
        LocalDate hoy = LocalDate.now();
        long disponible = loteRepository.stockDisponible(producto.getId(), hoy);
        if (disponible < cantidad) {
            throw new NegocioException(
                Mensajes.get(
                    "error.inventario.stockInsuficiente",
                    producto.getNombreComercial(), String.valueOf(disponible), String.valueOf(cantidad)
                )
            );
        }
        List<ConsumoLote> consumos = new ArrayList<>();
        int pendiente = cantidad;
        for (Lote lote : loteRepository.vigentesFefo(producto.getId(), hoy)) {
            if (pendiente == 0) {
                break;
            }
            int tomar = Math.min(pendiente, lote.getCantidadActual());
            registrarSalidaDeLote(lote, tomar, tipo, contexto);
            consumos.add(new ConsumoLote(lote, tomar));
            pendiente -= tomar;
        }
        return consumos;
    }

    /** Verifica el stock vendible de varios productos antes de mutar nada. */
    @Transactional(readOnly = true)
    public void validarDisponible(Map<Producto, Integer> solicitado) {
        LocalDate hoy = LocalDate.now();
        solicitado.forEach((producto, cantidad) -> {
            long disponible = loteRepository.stockDisponible(producto.getId(), hoy);
            if (disponible < cantidad) {
                throw new NegocioException(
                    Mensajes.get(
                        "error.inventario.stockInsuficiente",
                        producto.getNombreComercial(), String.valueOf(disponible), String.valueOf(cantidad)
                    )
                );
            }
        });
    }

    /** Da de baja unidades de un lote (vencidos, danados). Solo OWNER. */
    @Transactional
    public void registrarMerma(Long loteId, int cantidad, String motivo) {
        sesionActual.exigirOwner(Mensajes.get("error.inventario.sinPermiso"));
        Lote lote = loteRepository.findById(loteId)
            .orElseThrow(() -> new NegocioException(Mensajes.get("error.inventario.loteNoEncontrado")));
        String detalle = Textos.requerido("motivo", "el motivo", motivo, 40);
        Long usuarioId = sesionActual.usuarioAutenticado().map(u -> u.id()).orElse(null);
        registrarSalidaDeLote(lote, cantidad, TipoMovimiento.MERMA, MovimientoContexto.ajuste(detalle, usuarioId));
        log.info("Merma de {} unidades del lote {}", cantidad, lote.getCodigoLote());
    }

    public record ConsumoLote(Lote lote, int cantidad) {
    }

    /** Costo promedio ponderado a 4 decimales; si no habia stock, queda el costo nuevo. */
    public static BigDecimal costoPromedio(BigDecimal actual, long stock, BigDecimal nuevo, int cantidad) {
        if (actual == null || stock <= 0) {
            return nuevo.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal total = actual.multiply(BigDecimal.valueOf(stock))
            .add(nuevo.multiply(BigDecimal.valueOf(cantidad)));
        return total.divide(BigDecimal.valueOf(stock + cantidad), 4, RoundingMode.HALF_UP);
    }

    // ---------------- internos ----------------

    private List<InventarioFila> filtrar(String texto, FiltroStock filtro) {
        String limpio = Textos.limpiar(texto);
        String patron = limpio == null ? "" : "%" + limpio.toLowerCase() + "%";
        return consultaRepository.filas(patron).stream()
            .filter(fila -> switch (filtro == null ? FiltroStock.TODOS : filtro) {
                case TODOS -> true;
                case DISPONIBLE -> fila.estadoStock() == InventarioFila.EstadoStock.DISPONIBLE;
                case BAJO -> fila.estadoStock() == InventarioFila.EstadoStock.BAJO;
                case SIN_STOCK -> fila.estadoStock() == InventarioFila.EstadoStock.SIN_STOCK;
            })
            .toList();
    }

    private void registrarMovimiento(
        Producto producto, Lote lote, TipoMovimiento tipo, int cantidad, BigDecimal costo,
        MovimientoContexto contexto, long stockResultante
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setFecha(contexto.fecha());
        movimiento.setProducto(producto);
        movimiento.setLote(lote);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setStockResultante((int) stockResultante);
        movimiento.setCostoUnitario(costo);
        movimiento.setDocumento(contexto.documento());
        movimiento.setCompraId(contexto.compraId());
        movimiento.setVentaId(contexto.ventaId());
        movimiento.setUsuarioId(contexto.usuarioId());
        movimientoRepository.save(movimiento);
    }

    private LoteResumen resumen(Lote lote, LocalDate hoy) {
        LocalDate vencimiento = lote.getFechaVencimiento();
        long dias = vencimiento == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(hoy, vencimiento);
        return new LoteResumen(
            lote.getId(), lote.getProducto().getId(), lote.getProducto().getNombreComercial(),
            lote.getCodigoLote(), vencimiento, lote.getCantidadActual(), lote.estaVencido(hoy), dias
        );
    }
}
