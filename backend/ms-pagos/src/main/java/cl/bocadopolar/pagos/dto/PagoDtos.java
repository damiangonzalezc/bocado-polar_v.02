package cl.bocadopolar.pagos.dto;

import java.math.BigDecimal;

import cl.bocadopolar.pagos.model.MetodoPago;
import cl.bocadopolar.pagos.model.TipoCobro;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PagoDtos {

    private PagoDtos() {
    }

    /** tipoCobro y pagaCon solo aplican a CONTRA_ENTREGA. */
    public record IniciarPagoRequest(@NotNull Long pedidoId, @NotNull MetodoPago metodo,
            @Size(max = 64) String idempotencyKey, TipoCobro tipoCobro, @DecimalMin("0") BigDecimal pagaCon) {
    }

    /** Simula la confirmación que envía Webpay (o la tarjeta) al servidor. */
    public record WebhookRequest(@NotNull Long pagoId, @NotNull Boolean aprobado,
            @Size(max = 60) String codigoAutorizacion, @Size(max = 200) String motivo) {
    }

    public record CobroEntregaRequest(@NotNull Long repartidorId, @NotNull TipoCobro tipo,
            @DecimalMin("0") BigDecimal pagaCon) {
    }

    public record ReembolsoRequest(@NotBlank @Size(max = 300) String motivo, @DecimalMin("1") BigDecimal monto,
            Long solicitadoPorId) {
    }

    public record AprobarRequest(Long usuarioId) {
    }

    public record RechazarRequest(@NotBlank @Size(max = 300) String motivo, Long usuarioId) {
    }
}
