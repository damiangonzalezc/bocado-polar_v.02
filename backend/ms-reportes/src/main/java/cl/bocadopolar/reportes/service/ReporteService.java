package cl.bocadopolar.reportes.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.reportes.client.Clientes;
import cl.bocadopolar.reportes.client.Clientes.ItemDto;
import cl.bocadopolar.reportes.client.Clientes.PedidoDto;
import cl.bocadopolar.reportes.dto.ReporteDtos.AlertaOperativa;
import cl.bocadopolar.reportes.dto.ReporteDtos.Comparacion;
import cl.bocadopolar.reportes.dto.ReporteDtos.KpiResponse;
import cl.bocadopolar.reportes.dto.ReporteDtos.TopProducto;
import cl.bocadopolar.reportes.dto.ReporteDtos.VentaDiaria;
import cl.bocadopolar.reportes.model.KpiPeriodo;
import cl.bocadopolar.reportes.model.Periodo;
import cl.bocadopolar.reportes.model.TipoAlertaOperativa;
import cl.bocadopolar.reportes.model.VisitaDiaria;
import cl.bocadopolar.reportes.repository.KpiPeriodoRepository;
import cl.bocadopolar.reportes.repository.VisitaDiariaRepository;
import lombok.RequiredArgsConstructor;

/** Los KPI se calculan al vuelo con los pedidos de ms-pedidos (no cuentan cancelados, reembolsados ni sin pagar). */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final Set<String> NO_VENTA = Set.of("CANCELADO", "REEMBOLSADO", "PENDIENTE_PAGO");
    private static final Set<String> EN_PROCESO = Set.of("PAGADO", "EN_PREPARACION", "ENVIADO");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final Clientes clientes;
    private final VisitaDiariaRepository visitaRepository;
    private final KpiPeriodoRepository kpiRepository;

    /* =============== KPIs =============== */

    public KpiResponse kpis(Periodo periodo) {
        LocalDate[] r = rango(periodo);
        long dias = ChronoUnit.DAYS.between(r[0], r[1]) + 1;
        LocalDate desdeAnt = r[0].minusDays(dias);
        LocalDate hastaAnt = r[0].minusDays(1);

        List<PedidoDto> todos = clientes.pedidos(desdeAnt, r[1]);
        BigDecimal iva = clientes.iva();
        Calculo actual = calcular(todos, r[0], r[1], iva);
        Calculo anterior = calcular(todos, desdeAnt, hastaAnt, iva);
        Comparacion c = new Comparacion(desdeAnt, hastaAnt, variacion(actual.brutas, anterior.brutas),
                variacion(BigDecimal.valueOf(actual.ordenes), BigDecimal.valueOf(anterior.ordenes)),
                variacion(actual.ticket, anterior.ticket), variacion(actual.conversion, anterior.conversion));
        return new KpiResponse(periodo, r[0], r[1], actual.ordenes, actual.visitas, actual.conversion, actual.brutas,
                actual.netas, actual.ticket, c);
    }

    @Transactional
    public KpiPeriodo guardarSnapshot(Periodo periodo) {
        KpiResponse k = kpis(periodo);
        return kpiRepository.save(new KpiPeriodo(null, periodo, k.desde(), k.hasta(), k.ordenes(), k.visitas(),
                k.conversion(), k.ventasBrutas(), k.ventasNetas(), k.ticketPromedio(), Instant.now()));
    }

    public List<KpiPeriodo> historial() {
        return kpiRepository.findAllByOrderByCalculadoEnDesc();
    }

    /** CU-12.03 Productos más vendidos del período (por ingresos). */
    public List<TopProducto> topProductos(Periodo periodo, int limite) {
        LocalDate[] r = rango(periodo);
        Map<Long, TopProducto> acumulado = new LinkedHashMap<>();
        for (PedidoDto p : clientes.pedidos(r[0], r[1])) {
            if (NO_VENTA.contains(p.estado()) || p.items() == null) {
                continue;
            }
            for (ItemDto i : p.items()) {
                TopProducto t = acumulado.get(i.productoId());
                BigDecimal kg = i.kg() == null ? BigDecimal.ZERO : i.kg();
                BigDecimal ingreso = i.subtotal() == null ? BigDecimal.ZERO : i.subtotal();
                acumulado.put(i.productoId(), t == null ? new TopProducto(i.productoId(), i.nombre(), kg, ingreso)
                        : new TopProducto(i.productoId(), t.nombre(), t.kg().add(kg), t.ingresos().add(ingreso)));
            }
        }
        return acumulado.values().stream()
                .sorted(Comparator.comparing(TopProducto::ingresos).reversed())
                .limit(limite)
                .toList();
    }

    /** Serie diaria para el gráfico del panel. */
    public List<VentaDiaria> ventasDiarias(Periodo periodo) {
        LocalDate[] r = rango(periodo);
        ZoneId zona = ZoneId.systemDefault();
        Map<LocalDate, int[]> ordenes = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal> ventas = new LinkedHashMap<>();
        for (LocalDate d = r[0]; !d.isAfter(r[1]); d = d.plusDays(1)) {
            ordenes.put(d, new int[] { 0 });
            ventas.put(d, BigDecimal.ZERO);
        }
        for (PedidoDto p : clientes.pedidos(r[0], r[1])) {
            if (NO_VENTA.contains(p.estado()) || p.creadoEn() == null) {
                continue;
            }
            LocalDate d = p.creadoEn().atZone(zona).toLocalDate();
            if (ordenes.containsKey(d)) {
                ordenes.get(d)[0]++;
                ventas.put(d, ventas.get(d).add(p.total()));
            }
        }
        Map<LocalDate, Integer> visitas = new LinkedHashMap<>();
        visitaRepository.findByFechaBetweenOrderByFechaAsc(r[0], r[1]).forEach(v -> visitas.put(v.getFecha(), v.getVisitas()));
        List<VentaDiaria> serie = new ArrayList<>();
        ordenes.forEach((d, n) -> serie.add(new VentaDiaria(d, n[0], ventas.get(d), visitas.getOrDefault(d, 0))));
        return serie;
    }

    /** CU-12.04 Alertas operativas con enlace a la página del panel que las resuelve. */
    public List<AlertaOperativa> alertasOperativas() {
        LocalDate hoy = LocalDate.now();
        ZoneId zona = ZoneId.systemDefault();
        List<AlertaOperativa> lista = new ArrayList<>();
        lista.add(new AlertaOperativa(TipoAlertaOperativa.STOCK_CRITICO, clientes.contarAlertasStock(), "/admin/inventario"));
        lista.add(new AlertaOperativa(TipoAlertaOperativa.FRIO, clientes.contarAlertasFrio(), "/admin/frio"));
        int incidenciasHoy = (int) clientes.incidencias().stream()
                .filter(i -> i.fecha() != null && i.fecha().atZone(zona).toLocalDate().equals(hoy)).count();
        lista.add(new AlertaOperativa(TipoAlertaOperativa.INCIDENCIA, incidenciasHoy, "/admin/rutas"));
        int atrasados;
        try {
            atrasados = (int) clientes.pedidos(hoy.minusDays(60), hoy).stream()
                    .filter(p -> EN_PROCESO.contains(p.estado()) && p.fechaEntrega() != null && p.fechaEntrega().isBefore(hoy))
                    .count();
        } catch (RuntimeException e) {
            atrasados = -1;
        }
        lista.add(new AlertaOperativa(TipoAlertaOperativa.PEDIDO_ATRASADO, atrasados, "/admin/pedidos"));
        return lista;
    }

    /* =============== Visitas =============== */

    /** CU-12.05 Suma una visita de hoy (y al canal de origen en ms-marketing si viene). */
    @Transactional
    public VisitaDiaria registrarVisita(String canal) {
        LocalDate hoy = LocalDate.now();
        VisitaDiaria v = visitaRepository.findById(hoy).orElseGet(() -> new VisitaDiaria(hoy, 0));
        v.setVisitas(v.getVisitas() + 1);
        if (canal != null && !canal.isBlank()) {
            clientes.visitaCanal(canal.trim());
        }
        return visitaRepository.save(v);
    }

    public List<VisitaDiaria> visitas(LocalDate desde, LocalDate hasta) {
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        LocalDate d = desde == null ? h.minusDays(29) : desde;
        return visitaRepository.findByFechaBetweenOrderByFechaAsc(d, h);
    }

    /* =============== internos =============== */

    static LocalDate[] rango(Periodo periodo) {
        LocalDate hoy = LocalDate.now();
        return switch (periodo) {
            case HOY -> new LocalDate[] { hoy, hoy };
            case SIETE_DIAS -> new LocalDate[] { hoy.minusDays(6), hoy };
            case MES_ACTUAL -> new LocalDate[] { hoy.withDayOfMonth(1), hoy };
            case TREINTA_DIAS -> new LocalDate[] { hoy.minusDays(29), hoy };
        };
    }

    private Calculo calcular(List<PedidoDto> pedidos, LocalDate desde, LocalDate hasta, BigDecimal iva) {
        ZoneId zona = ZoneId.systemDefault();
        int ordenes = 0;
        BigDecimal brutas = BigDecimal.ZERO;
        for (PedidoDto p : pedidos) {
            if (NO_VENTA.contains(p.estado()) || p.creadoEn() == null) {
                continue;
            }
            LocalDate d = p.creadoEn().atZone(zona).toLocalDate();
            if (!d.isBefore(desde) && !d.isAfter(hasta)) {
                ordenes++;
                brutas = brutas.add(p.total() == null ? BigDecimal.ZERO : p.total());
            }
        }
        int visitas = visitaRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta).stream()
                .mapToInt(VisitaDiaria::getVisitas).sum();
        BigDecimal netas = brutas.divide(BigDecimal.ONE.add(iva.divide(CIEN, 4, RoundingMode.HALF_UP)), 0, RoundingMode.HALF_UP);
        BigDecimal ticket = ordenes == 0 ? BigDecimal.ZERO : brutas.divide(BigDecimal.valueOf(ordenes), 0, RoundingMode.HALF_UP);
        BigDecimal conversion = visitas == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(ordenes).multiply(CIEN).divide(BigDecimal.valueOf(visitas), 2, RoundingMode.HALF_UP);
        return new Calculo(ordenes, visitas, brutas, netas, ticket, conversion);
    }

    private static BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return actual.subtract(anterior).multiply(CIEN).divide(anterior, 1, RoundingMode.HALF_UP);
    }

    private record Calculo(int ordenes, int visitas, BigDecimal brutas, BigDecimal netas, BigDecimal ticket,
            BigDecimal conversion) {
    }
}
