package cl.bocadopolar.reparto.dto;

import java.math.BigDecimal;

import cl.bocadopolar.reparto.model.Disponibilidad;
import cl.bocadopolar.reparto.model.TipoIncidencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class RepartoDtos {

    private RepartoDtos() {
    }

    /** id = id de usuario en ms-auth. */
    public record RepartidorRequest(@NotNull Long usuarioId, @NotBlank @Size(max = 80) String nombre,
            @Size(max = 120) String email, @Size(max = 12) String rut) {
    }

    public record DisponibilidadRequest(@NotNull Disponibilidad disponibilidad) {
    }

    public record PreferenciasRequest(@NotNull Boolean sonido, @NotNull Boolean vibracion) {
    }

    public record IniciarTurnoRequest(@NotNull Long repartidorId,
            @NotNull @DecimalMin("-40") @DecimalMax("15") Double tempCajaInicial) {
    }

    public record CerrarTurnoRequest(@NotNull @DecimalMin("0") BigDecimal efectivoRendido) {
    }

    /** tipo: EFECTIVO | POS_MOVIL | TRANSFERENCIA */
    public record CobroRequest(@NotBlank @Pattern(regexp = "EFECTIVO|POS_MOVIL|TRANSFERENCIA") String tipo,
            @DecimalMin("0") BigDecimal pagaCon) {
    }

    /** Se exige fotoUrl o firmaUrl. Si el pedido es contra entrega, se exige cobro. */
    public record EntregaRequest(@NotNull Long repartidorId, Long turnoId, @NotNull Long pedidoId, Long paradaId,
            @NotBlank @Size(max = 80) String recibidoPor, @DecimalMin("-40") @DecimalMax("15") Double tempEntrega,
            String fotoUrl, String firmaUrl, @Size(max = 300) String comentario, @Valid CobroRequest cobro) {
    }

    public record EntregaFallidaRequest(@NotNull Long repartidorId, Long turnoId, @NotNull Long pedidoId, Long paradaId,
            @NotNull TipoIncidencia tipo, @NotBlank @Size(max = 300) String nota) {
    }

    public record IncidenciaRequest(@NotNull TipoIncidencia tipo, @NotBlank @Size(max = 300) String nota, Long entregaId,
            Long pedidoId, @NotNull Long reportadaPorId) {
    }

    public record CalificacionRequest(Long clienteId, @NotNull @Min(1) @Max(5) Integer estrellas) {
    }
}
