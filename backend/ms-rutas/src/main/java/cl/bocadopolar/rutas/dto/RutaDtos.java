package cl.bocadopolar.rutas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class RutaDtos {

    private RutaDtos() {
    }

    /** horaSalida en formato "08:30". */
    public record RutaRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotNull LocalDate fecha,
            @NotEmpty List<@NotBlank String> comunas,
            @NotNull LocalTime horaSalida,
            Long repartidorId,
            @Size(max = 80) String repartidorNombre,
            Long vehiculoId,
            @Size(max = 300) String notas) {
    }

    public record AsignacionRequest(@NotNull Long repartidorId, @Size(max = 80) String repartidorNombre,
            @NotNull Long vehiculoId) {
    }

    public record VehiculoRequest(
            @NotBlank @Size(max = 40) String tipo,
            @NotBlank @Pattern(regexp = "^[A-Za-z]{2,4}-?\\d{2,4}$", message = "Patente inválida. Ejemplo: KLTY-21 o AB-1234") String patente,
            Boolean refrigerado,
            Boolean activo) {
    }

    public record ParadaRequest(@NotNull Long pedidoId, @Size(max = 20) String codigoPedido, @Size(max = 80) String cliente,
            @NotBlank @Size(max = 100) String direccion, @NotBlank @Size(max = 60) String comuna,
            @Size(max = 120) String referencia, @Size(max = 20) String horario, Boolean cobrarAlEntregar,
            BigDecimal montoACobrar) {
    }

    /** El admin asigna un pedido a un repartidor: se agrega a la ruta de ese repartidor para la fecha de entrega. */
    public record AsignarPedidoRequest(@NotNull Long pedidoId, @NotNull Long repartidorId,
            @Size(max = 80) String repartidorNombre, Long usuarioId) {
    }

    public record FallaRequest(@NotBlank @Size(max = 200) String motivo) {
    }
}
