package cl.bocadopolar.inventario.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Publica avisos en ms-notificaciones (de mejor esfuerzo: si falla no detiene la operación). */
@Component
public class NotificacionesClient {

    private final RestClient http;

    public NotificacionesClient(RestClient.Builder builder, @Value("${bocado.servicios.notificaciones}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public void enviar(String motivo, String canal, String destinatario, Map<String, Object> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("eventoId", UUID.randomUUID().toString());
        body.put("motivo", motivo);
        body.put("canal", canal);
        body.put("destinatario", destinatario);
        body.put("variables", variables);
        Remoto.intentar("ms-notificaciones",
                () -> http.post().uri("/api/v1/notificaciones/eventos").body(body).retrieve().toBodilessEntity());
    }
}
