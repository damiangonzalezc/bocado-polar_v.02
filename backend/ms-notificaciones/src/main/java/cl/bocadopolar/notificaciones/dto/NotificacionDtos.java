package cl.bocadopolar.notificaciones.dto;

import java.util.Map;

import cl.bocadopolar.notificaciones.model.Canal;
import cl.bocadopolar.notificaciones.model.MotivoNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class NotificacionDtos {

    private NotificacionDtos() {
    }

    /**
     * Evento que envían los demás microservicios.
     * destinatario: un correo para CORREO, o "admin" para PANEL_ADMIN.
     */
    public record EventoRequest(@Size(max = 64) String eventoId, @NotNull MotivoNotificacion motivo, Canal canal,
            @NotBlank @Size(max = 120) String destinatario, Map<String, Object> variables) {
    }

    public record PlantillaRequest(@NotNull MotivoNotificacion motivo, @NotBlank @Size(max = 150) String asunto,
            @NotBlank String cuerpo, Boolean activa) {
    }
}
