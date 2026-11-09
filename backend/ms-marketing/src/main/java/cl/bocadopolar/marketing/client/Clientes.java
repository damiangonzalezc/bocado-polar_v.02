package cl.bocadopolar.marketing.client;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Clientes REST de ms-marketing: ms-pedidos (carritos), ms-clientes (correo) y ms-notificaciones. */
public final class Clientes {

    private Clientes() {
    }

    @Component
    public static class PedidosClient {
        private final RestClient http;

        public PedidosClient(RestClient.Builder builder, @Value("${bocado.servicios.pedidos}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public List<CarritoDto> carritosInactivos(int horas) {
            return Remoto.ejecutar("ms-pedidos", () -> http.get()
                    .uri("/api/v1/carritos/inactivos?horas={h}", horas)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CarritoDto>>() {
                    }));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CarritoDto(Long id, Long clienteId, BigDecimal subtotal, List<ItemDto> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemDto(String nombre, BigDecimal kg) {
    }

    @Component
    public static class ClientesClient {
        private final RestClient http;

        public ClientesClient(RestClient.Builder builder, @Value("${bocado.servicios.clientes}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        /** null si no se encuentra o ms-clientes no responde. */
        public ClienteDto cliente(Long id) {
            try {
                return http.get().uri("/api/v1/clientes/{id}", id).retrieve().body(ClienteDto.class);
            } catch (Exception e) {
                return null;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClienteDto(Long id, String nombre, String apellido, String email) {
    }

    @Component
    public static class NotificacionesClient {
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
            Remoto.ejecutar("ms-notificaciones",
                    () -> http.post().uri("/api/v1/notificaciones/eventos").body(body).retrieve().toBodilessEntity());
        }
    }
}
