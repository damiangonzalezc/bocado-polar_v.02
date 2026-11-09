package cl.bocadopolar.reparto.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Clientes REST de ms-reparto: rutas, cadena de frío, pagos, pedidos y notificaciones. */
public final class Clientes {

    private Clientes() {
    }

    /* ------------------------------------------------------------------ ms-rutas */
    @Component
    public static class RutasClient {
        private final RestClient http;

        public RutasClient(RestClient.Builder builder, @Value("${bocado.servicios.rutas}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        /** null si no tiene ruta hoy o ms-rutas no responde. */
        public RutaDto rutaDelDia(Long repartidorId) {
            try {
                return http.get().uri("/api/v1/rutas/repartidor/{id}/hoy", repartidorId).retrieve().body(RutaDto.class);
            } catch (Exception e) {
                return null;
            }
        }

        public void accionRuta(Long rutaId, String accion) {
            Remoto.intentar("ms-rutas", () -> http.patch().uri("/api/v1/rutas/{id}/" + accion, rutaId)
                    .retrieve().toBodilessEntity());
        }

        public void paradaEntregada(Long paradaId) {
            Remoto.intentar("ms-rutas", () -> http.patch().uri("/api/v1/paradas/{id}/entregada", paradaId)
                    .retrieve().toBodilessEntity());
        }

        public void paradaFallida(Long paradaId, String motivo) {
            Remoto.intentar("ms-rutas", () -> http.patch().uri("/api/v1/paradas/{id}/fallida", paradaId)
                    .body(Map.of("motivo", motivo)).retrieve().toBodilessEntity());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RutaDto(Long id, String codigo, String estado) {
    }

    /* ------------------------------------------------------------------ ms-frio */
    @Component
    public static class FrioClient {
        private final RestClient http;

        public FrioClient(RestClient.Builder builder, @Value("${bocado.servicios.frio}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        /** Registra la temperatura y devuelve true si generó alerta (o null si ms-frio no respondió). */
        public Boolean registrarTemperatura(String origen, Long referenciaId, Double grados, Long registradoPorId) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("origen", origen);
                body.put("referenciaId", referenciaId);
                body.put("grados", grados);
                body.put("registradoPorId", registradoPorId);
                RegistroDto r = http.post().uri("/api/v1/temperaturas").body(body).retrieve().body(RegistroDto.class);
                return r != null && r.alerta() != null;
            } catch (Exception e) {
                return null;
            }
        }

        public void iniciarCuentaRegresiva(Long turnoId) {
            Remoto.intentar("ms-frio", () -> http.post().uri("/api/v1/cuentas-regresivas")
                    .body(Map.of("turnoId", turnoId)).retrieve().toBodilessEntity());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RegistroDto(Object registro, Object alerta) {
    }

    /* ------------------------------------------------------------------ ms-pagos */
    @Component
    public static class PagosClient {
        private final RestClient http;

        public PagosClient(RestClient.Builder builder, @Value("${bocado.servicios.pagos}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public List<PagoDto> pagosDelPedido(Long pedidoId) {
            return Remoto.ejecutar("ms-pagos", () -> http.get().uri("/api/v1/pagos/pedido/{id}", pedidoId)
                    .retrieve().body(new ParameterizedTypeReference<List<PagoDto>>() {
                    }));
        }

        public void cobrar(Long pagoId, Long repartidorId, String tipo, BigDecimal pagaCon) {
            Map<String, Object> body = new HashMap<>();
            body.put("repartidorId", repartidorId);
            body.put("tipo", tipo);
            body.put("pagaCon", pagaCon);
            Remoto.ejecutar("ms-pagos", () -> http.patch().uri("/api/v1/pagos/{id}/cobro-entrega", pagoId)
                    .body(body).retrieve().toBodilessEntity());
        }

        public List<CobroDto> cobrosDelDia(Long repartidorId, LocalDate fecha) {
            try {
                return http.get().uri("/api/v1/pagos/cobros?repartidorId={r}&fecha={f}", repartidorId, fecha.toString())
                        .retrieve().body(new ParameterizedTypeReference<List<CobroDto>>() {
                        });
            } catch (Exception e) {
                return List.of();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PagoDto(Long id, String metodo, String estado, BigDecimal monto) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CobroDto(Long pagoId, String tipo, BigDecimal monto, Boolean cobrado) {
    }

    /* ------------------------------------------------------------------ ms-pedidos */
    @Component
    public static class PedidosClient {
        private final RestClient http;

        public PedidosClient(RestClient.Builder builder, @Value("${bocado.servicios.pedidos}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public void marcarEntregado(Long pedidoId, String nota, Long usuarioId) {
            Map<String, Object> body = new HashMap<>();
            body.put("estado", "ENTREGADO");
            body.put("nota", nota);
            body.put("usuarioId", usuarioId);
            Remoto.ejecutar("ms-pedidos", () -> http.patch().uri("/api/v1/pedidos/{id}/estado", pedidoId)
                    .body(body).retrieve().toBodilessEntity());
        }

        public void agregarNota(Long pedidoId, String texto, Long autorId) {
            Map<String, Object> body = new HashMap<>();
            body.put("texto", texto);
            body.put("autorId", autorId);
            Remoto.intentar("ms-pedidos", () -> http.post().uri("/api/v1/pedidos/{id}/notas", pedidoId)
                    .body(body).retrieve().toBodilessEntity());
        }
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
