package cl.bocadopolar.frio.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Bloquea lotes en ms-inventario y publica alertas en ms-notificaciones. */
@Component
public class InventarioClient {

    private final RestClient inventario;
    private final RestClient notificaciones;

    public InventarioClient(RestClient.Builder builder, @Value("${bocado.servicios.inventario}") String urlInventario,
            @Value("${bocado.servicios.notificaciones}") String urlNotificaciones) {
        this.inventario = builder.clone().baseUrl(urlInventario).build();
        this.notificaciones = builder.clone().baseUrl(urlNotificaciones).build();
    }

    public void bloquearLote(Long loteId, String motivo) {
        Remoto.intentar("ms-inventario", () -> inventario.patch().uri("/api/v1/lotes/{id}/bloqueo", loteId)
                .body(Map.of("motivo", motivo)).retrieve().toBodilessEntity());
    }

    public void alertar(Map<String, Object> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("eventoId", UUID.randomUUID().toString());
        body.put("motivo", "ALERTA_FRIO");
        body.put("canal", "PANEL_ADMIN");
        body.put("destinatario", "admin");
        body.put("variables", variables);
        Remoto.intentar("ms-notificaciones", () -> notificaciones.post().uri("/api/v1/notificaciones/eventos")
                .body(body).retrieve().toBodilessEntity());
    }
}
