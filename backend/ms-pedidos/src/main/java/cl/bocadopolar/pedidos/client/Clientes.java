package cl.bocadopolar.pedidos.client;

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

import lombok.extern.slf4j.Slf4j;

/**
 * Clientes REST hacia los otros microservicios que usa ms-pedidos.
 * Las clases están juntas para que se vea en un solo lugar de quién depende el servicio.
 */
public final class Clientes {

    private Clientes() {
    }

    /* ------------------------------------------------------------------ ms-catalogo */
    @Component
    public static class CatalogoClient {
        private final RestClient http;

        public CatalogoClient(RestClient.Builder builder, @Value("${bocado.servicios.catalogo}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public List<ProductoDto> productos(List<Long> ids) {
            String csv = String.join(",", ids.stream().map(String::valueOf).toList());
            return Remoto.ejecutar("ms-catalogo", () -> http.get()
                    .uri("/api/v1/productos/por-ids?ids={ids}", csv)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ProductoDto>>() {
                    }));
        }

        public ProductoDto producto(Long id) {
            return Remoto.ejecutar("ms-catalogo",
                    () -> http.get().uri("/api/v1/productos/{id}", id).retrieve().body(ProductoDto.class));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductoDto(Long id, String nombre, BigDecimal precioKg, Boolean activo, String imagen) {
    }

    /* ------------------------------------------------------------------ ms-config */
    @Component
    public static class ConfigClient {
        private final RestClient http;

        public ConfigClient(RestClient.Builder builder, @Value("${bocado.servicios.config}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public BigDecimal tarifa(String comuna) {
            TarifaDto t = Remoto.ejecutar("ms-config",
                    () -> http.get().uri("/api/v1/zonas/tarifa?comuna={c}", comuna).retrieve().body(TarifaDto.class));
            return t == null || t.tarifa() == null ? BigDecimal.ZERO : t.tarifa();
        }

        public BigDecimal regla(String codigo, BigDecimal porDefecto) {
            try {
                ReglaDto r = http.get().uri("/api/v1/reglas/{c}", codigo).retrieve().body(ReglaDto.class);
                return r != null && r.valor() != null ? r.valor() : porDefecto;
            } catch (Exception e) {
                return porDefecto;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TarifaDto(String comuna, BigDecimal tarifa) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReglaDto(String codigo, BigDecimal valor) {
    }

    /* ------------------------------------------------------------------ ms-marketing */
    @Component
    public static class MarketingClient {
        private final RestClient http;

        public MarketingClient(RestClient.Builder builder, @Value("${bocado.servicios.marketing}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public CuponDto validar(String codigo, Long clienteId, BigDecimal subtotal) {
            Map<String, Object> body = new HashMap<>();
            body.put("codigo", codigo);
            body.put("clienteId", clienteId);
            body.put("subtotal", subtotal);
            return Remoto.ejecutar("ms-marketing",
                    () -> http.post().uri("/api/v1/cupones/validar").body(body).retrieve().body(CuponDto.class));
        }

        public void registrarUso(String codigo, Long pedidoId, Long clienteId) {
            Map<String, Object> body = new HashMap<>();
            body.put("codigo", codigo);
            body.put("pedidoId", pedidoId);
            body.put("clienteId", clienteId);
            Remoto.intentar("ms-marketing",
                    () -> http.post().uri("/api/v1/cupones/usos").body(body).retrieve().toBodilessEntity());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CuponDto(Boolean valido, String codigo, BigDecimal descuento, String mensaje) {
    }

    /* ------------------------------------------------------------------ ms-inventario */
    @Slf4j
    @Component
    public static class InventarioClient {
        private final RestClient http;

        public InventarioClient(RestClient.Builder builder, @Value("${bocado.servicios.inventario}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public void reservar(Long pedidoId, List<Map<String, Object>> items) {
            Map<String, Object> body = Map.of("pedidoId", pedidoId, "items", items);
            Remoto.ejecutar("ms-inventario",
                    () -> http.post().uri("/api/v1/reservas").body(body).retrieve().toBodilessEntity());
        }

        public void confirmar(Long pedidoId) {
            Remoto.ejecutar("ms-inventario", () -> http.patch()
                    .uri("/api/v1/reservas/pedido/{id}/confirmar", pedidoId).retrieve().toBodilessEntity());
        }

        public void liberar(Long pedidoId) {
            Remoto.intentar("ms-inventario", () -> http.patch()
                    .uri("/api/v1/reservas/pedido/{id}/liberar", pedidoId).retrieve().toBodilessEntity());
        }
    }

    /* ------------------------------------------------------------------ ms-clientes */
    @Component
    public static class ClientesClient {
        private final RestClient http;

        public ClientesClient(RestClient.Builder builder, @Value("${bocado.servicios.clientes}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public void registrarCompra(Long clienteId, BigDecimal monto, Long pedidoId) {
            Map<String, Object> body = new HashMap<>();
            body.put("monto", monto);
            body.put("pedidoId", pedidoId);
            Remoto.intentar("ms-clientes", () -> http.post().uri("/api/v1/clientes/{id}/compras", clienteId)
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
