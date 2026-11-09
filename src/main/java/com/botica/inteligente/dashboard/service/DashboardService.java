package com.botica.inteligente.dashboard.service;

import com.botica.inteligente.dashboard.repository.ReporteVentasRepository;
import com.botica.inteligente.inventario.service.InventarioFila;
import com.botica.inteligente.inventario.service.InventarioService;
import com.botica.inteligente.inventario.service.LoteResumen;
import com.botica.inteligente.security.SesionActual;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final String SIN_PERMISO = "Solo el propietario puede ver los indicadores de ventas.";
    private static final int DIAS_MAXIMOS_SERIE_DIARIA = 62;
    private static final int MAS_VENDIDOS = 8;
    private static final int DIAS_ALERTA_VENCIMIENTO = 30;
    private static final int FILAS_ALERTA = 8;
    private static final DateTimeFormatter ETIQUETA_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter ETIQUETA_MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ReporteVentasRepository reporteRepository;
    private final InventarioService inventarioService;
    private final SesionActual sesionActual;

    public DashboardService(
        ReporteVentasRepository reporteRepository, InventarioService inventarioService, SesionActual sesionActual
    ) {
        this.reporteRepository = reporteRepository;
        this.inventarioService = inventarioService;
        this.sesionActual = sesionActual;
    }

    @Transactional(readOnly = true)
    public ResumenVentas resumenVentas(Periodo periodo) {
        return resumenVentas(periodo, LocalDate.now());
    }

    @Transactional(readOnly = true)
    ResumenVentas resumenVentas(Periodo periodo, LocalDate hoy) {
        sesionActual.exigirOwner(SIN_PERMISO);
        LocalDate desde = periodo.desde(hoy);
        long dias = ChronoUnit.DAYS.between(desde, hoy) + 1;

        List<PuntoVenta> ventas = reporteRepository.ventasEmitidas(desde.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        BigDecimal total = sumar(ventas);
        BigDecimal ticket = ventas.isEmpty()
            ? BigDecimal.ZERO.setScale(2)
            : total.divide(BigDecimal.valueOf(ventas.size()), 2, RoundingMode.HALF_UP);

        BigDecimal ingreso = reporteRepository.ingresoSinIgv(desde.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        BigDecimal costo = reporteRepository.costoVendido(desde.atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        BigDecimal utilidad = ingreso.subtract(costo).setScale(2, RoundingMode.HALF_UP);

        LocalDate desdeAnterior = desde.minusDays(dias);
        List<PuntoVenta> anteriores = reporteRepository.ventasEmitidas(
            desdeAnterior.atStartOfDay(), desde.atStartOfDay()
        );
        BigDecimal totalAnterior = sumar(anteriores);
        BigDecimal variacion = totalAnterior.signum() == 0
            ? null
            : total.subtract(totalAnterior).multiply(BigDecimal.valueOf(100))
                .divide(totalAnterior, 1, RoundingMode.HALF_UP);

        return new ResumenVentas(
            periodo, desde, hoy, total, ventas.size(), ticket, utilidad, totalAnterior, variacion,
            serie(ventas, desde, hoy, dias),
            reporteRepository.masVendidos(
                desde.atStartOfDay(), hoy.plusDays(1).atStartOfDay(), PageRequest.of(0, MAS_VENDIDOS)
            )
        );
    }

    /** Alertas de stock y vencimiento; las ven el propietario y los vendedores. */
    @Transactional(readOnly = true)
    public Alertas alertas() {
        sesionActual.exigirAutenticado();
        List<InventarioFila> stock = inventarioService.alertasStockMinimo();
        List<LoteResumen> vencimientos = inventarioService.alertasVencimiento(DIAS_ALERTA_VENCIMIENTO);
        int sinStock = (int) stock.stream().filter(f -> f.estadoStock() == InventarioFila.EstadoStock.SIN_STOCK).count();
        int vencidos = (int) vencimientos.stream().filter(LoteResumen::vencido).count();
        return new Alertas(
            stock.size() - sinStock, sinStock, vencimientos.size() - vencidos, vencidos,
            stock.stream().limit(FILAS_ALERTA).toList(), vencimientos.stream().limit(FILAS_ALERTA).toList()
        );
    }

    private static BigDecimal sumar(List<PuntoVenta> ventas) {
        return ventas.stream().map(PuntoVenta::total).reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /** Serie diaria (o mensual si el periodo es largo) con ceros en los dias sin ventas. */
    private static List<ResumenVentas.PuntoSerie> serie(List<PuntoVenta> ventas, LocalDate desde, LocalDate hasta, long dias) {
        boolean diaria = dias <= DIAS_MAXIMOS_SERIE_DIARIA;
        Map<String, BigDecimal> acumulado = new LinkedHashMap<>();
        if (diaria) {
            for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
                acumulado.put(ETIQUETA_DIA.format(dia), BigDecimal.ZERO);
            }
        } else {
            for (LocalDate mes = desde.withDayOfMonth(1); !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
                acumulado.put(ETIQUETA_MES.format(mes), BigDecimal.ZERO);
            }
        }
        for (PuntoVenta venta : ventas) {
            String clave = (diaria ? ETIQUETA_DIA : ETIQUETA_MES).format(venta.fecha());
            acumulado.merge(clave, venta.total(), BigDecimal::add);
        }
        List<ResumenVentas.PuntoSerie> serie = new ArrayList<>();
        acumulado.forEach((etiqueta, valor) -> serie.add(new ResumenVentas.PuntoSerie(etiqueta, valor)));
        return serie;
    }
}
