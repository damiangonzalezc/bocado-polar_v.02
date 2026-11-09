package cl.bocadopolar.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.bocadopolar.reportes.model.Periodo;
import cl.bocadopolar.reportes.model.TipoAlertaOperativa;
import jakarta.validation.constraints.Size;

public final class ReporteDtos {

    private ReporteDtos() {
    }

    /** variaciones = % de cambio contra el período anterior de igual largo (null si el anterior fue 0). */
    public record KpiResponse(Periodo periodo, LocalDate desde, LocalDate hasta, int ordenes, int visitas,
            BigDecimal conversion, BigDecimal ventasBrutas, BigDecimal ventasNetas, BigDecimal ticketPromedio,
            Comparacion comparacion) {
    }

    public record Comparacion(LocalDate desdeAnterior, LocalDate hastaAnterior, BigDecimal ventasBrutasPct,
            BigDecimal ordenesPct, BigDecimal ticketPromedioPct, BigDecimal conversionPct) {
    }

    public record TopProducto(Long productoId, String nombre, BigDecimal kg, BigDecimal ingresos) {
    }

    public record AlertaOperativa(TipoAlertaOperativa tipo, int cantidad, String enlace) {
    }

    public record VentaDiaria(LocalDate fecha, int ordenes, BigDecimal ventas, int visitas) {
    }

    public record VisitaRequest(@Size(max = 40) String canal) {
    }
}
