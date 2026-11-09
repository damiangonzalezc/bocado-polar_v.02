package cl.bocadopolar.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import cl.bocadopolar.inventario.model.TipoMovimiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class InventarioDtos {

    public static final String LOTE_REGEX = "^[A-Z0-9-]{4,16}$";
    public static final String LOTE_MSG = "4 a 16 MAYÚSCULAS, números o guiones (ej: SAL-2609-A)";

    private InventarioDtos() {
    }

    public record LoteRequest(
            @NotNull Long productoId,
            @NotBlank @Pattern(regexp = LOTE_REGEX, message = LOTE_MSG) String codigoLote,
            @NotNull @DecimalMin(value = "0.5", message = "Mínimo 0,5 kg") BigDecimal stockKg,
            @NotNull LocalDate vencimiento,
            Double tempAlmacenaje,
            Long usuarioId) {
    }

    public record LoteUpdateRequest(
            @NotBlank @Pattern(regexp = LOTE_REGEX, message = LOTE_MSG) String codigoLote,
            @NotNull LocalDate vencimiento,
            Double tempAlmacenaje) {
    }

    /**
     * ENTRADA suma kg, SALIDA y MERMA restan kg, AJUSTE acepta kg con signo (+ o -).
     */
    public record AjusteRequest(@NotNull TipoMovimiento tipo, @NotNull BigDecimal kg,
            @NotBlank @Size(max = 200) String motivo, Long usuarioId) {
    }

    public record AjusteMasivoItem(@NotNull Long loteId, @NotNull TipoMovimiento tipo, @NotNull BigDecimal kg,
            @NotBlank @Size(max = 200) String motivo) {
    }

    public record AjusteMasivoRequest(@NotEmpty List<@Valid AjusteMasivoItem> ajustes, Long usuarioId) {
    }

    public record BloqueoRequest(@NotBlank @Size(max = 200) String motivo) {
    }

    public record ItemReserva(@NotNull Long productoId, @NotNull @DecimalMin("0.5") BigDecimal kg) {
    }

    /** Lo envía ms-pedidos en el checkout. */
    public record ReservaRequest(@NotNull Long pedidoId, @NotEmpty List<@Valid ItemReserva> items) {
    }

    public record StockProducto(Long productoId, String productoNombre, BigDecimal stockKg, BigDecimal reservadoKg,
            BigDecimal disponibleKg, int lotes, boolean critico, LocalDate proximoVencimiento) {
    }
}
