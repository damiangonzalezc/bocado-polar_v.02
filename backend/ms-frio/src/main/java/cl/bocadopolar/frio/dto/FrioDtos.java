package cl.bocadopolar.frio.dto;

import cl.bocadopolar.frio.model.AlertaFrio;
import cl.bocadopolar.frio.model.OrigenTemp;
import cl.bocadopolar.frio.model.RegistroTemperatura;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public final class FrioDtos {

    private FrioDtos() {
    }

    public record TemperaturaRequest(@NotNull OrigenTemp origen, @NotNull Long referenciaId,
            @NotNull @DecimalMin("-60") @DecimalMax("30") Double grados, Long registradoPorId) {
    }

    /** alerta viene null si la temperatura está dentro del rango. */
    public record RegistroResponse(RegistroTemperatura registro, AlertaFrio alerta) {
    }

    public record CuentaRequest(@NotNull Long turnoId) {
    }

    public record AtenderRequest(Long usuarioId) {
    }
}
