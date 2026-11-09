package cl.bocadopolar.auth.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.bocadopolar.auth.model.Usuario;

/** Al crear un usuario REPARTIDOR se registra su ficha en ms-reparto. */
@Component
public class RepartoClient {

    private final RestClient http;

    public RepartoClient(RestClient.Builder builder, @Value("${bocado.servicios.reparto}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public void crearRepartidor(Usuario u) {
        Remoto.intentar("ms-reparto", () -> http.post()
                .uri("/api/v1/repartidores")
                .body(Map.of(
                        "usuarioId", u.getId(),
                        "nombre", u.nombreCompleto(),
                        "email", u.getEmail(),
                        "rut", u.getRut() == null ? "" : u.getRut()))
                .retrieve()
                .toBodilessEntity());
    }
}
