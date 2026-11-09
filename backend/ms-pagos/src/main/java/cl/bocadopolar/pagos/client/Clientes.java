package cl.bocadopolar.pagos.client;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Clientes REST de ms-pagos: ms-pedidos, ms-config y ms-notificaciones. */
public final class Clientes {

    private Clientes() {
    }

    /* ------------------------------------------------------------------ ms-pedidos */
    @Component
    public static class PedidosClient {
        private final RestClient http;

        public PedidosClient(RestClient.Builder builder, @Value("${bocado.servicios.pedidos}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public PedidoDto pedido(Long id) {
            return Remoto.ejecutar("ms-pedidos",
                    () -> http.get().uri("/api/v1/pedidos/{id}", id).retrieve().body(PedidoDto.class));
        }

        /** evento: APROBADO | RECHAZADO | CONTRA_ENTREGA | REEMBOLSADO */
        public void notificarPago(Long pedidoId, String evento, Long pagoId, String detalle) {
            Map<String, Object> body = new HashMap<>();
            body.put("evento", evento);
            body.put("pagoId", pagoId);
            body.put("detalle", detalle);
            Remoto.ejecutar("ms-pedidos", () -> http.patch().uri("/api/v1/pedidos/{id}/pago", pedidoId)
                    .body(body).retrieve().toBodilessEntity());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PedidoDto(Long id, String codigo, Long clienteId, String nombreCliente, String email,
            String direccion, String comuna, String estado, String metodoPago, BigDecimal subtotal,
            BigDecimal envio, BigDecimal descuento, BigDecimal total, List<ItemDto> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemDto(Long productoId, String nombre, BigDecimal precioKg, BigDecimal kg, BigDecimal subtotal) {
    }

    /* ------------------------------------------------------------------ ms-config */
    @Component
    public static class ConfigClient {
        private final RestClient http;

        public ConfigClient(RestClient.Builder builder, @Value("${bocado.servicios.config}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public boolean pasarelaDisponible(String nombre) {
            DisponibleDto d = Remoto.ejecutar("ms-config", () -> http.get()
                    .uri("/api/v1/pasarelas/disponible?nombre={n}", nombre).retrieve().body(DisponibleDto.class));
            return d != null && Boolean.TRUE.equals(d.conectada());
        }

        public BigDecimal iva() {
            try {
                IvaDto d = http.get().uri("/api/v1/configuracion/iva").retrieve().body(IvaDto.class);
                return d == null || d.iva() == null ? new BigDecimal("19") : d.iva();
            } catch (Exception e) {
                return new BigDecimal("19");
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DisponibleDto(String nombre, Boolean conectada) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IvaDto(BigDecimal iva) {
    }

    /* ------------------------------------------------------------------ ms-notificaciones */
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
            Remoto.intentar("ms-notificaciones",
                    () -> http.post().uri("/api/v1/notificaciones/eventos").body(body).retrieve().toBodilessEntity());
        }
    }
}
