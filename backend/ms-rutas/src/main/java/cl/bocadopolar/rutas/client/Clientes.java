package cl.bocadopolar.rutas.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Clientes REST de ms-rutas: ms-pedidos (dependencia "pedidos") y ms-reparto (nombre del repartidor). */
public final class Clientes {

    private Clientes() {
    }

    @Component
    public static class PedidosClient {
        private final RestClient http;

        public PedidosClient(RestClient.Builder builder, @Value("${bocado.servicios.pedidos}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public List<PedidoDto> pedidosPara(List<String> comunas, LocalDate fecha) {
            String csv = String.join(",", comunas);
            return Remoto.ejecutar("ms-pedidos", () -> http.get()
                    .uri("/api/v1/pedidos?comunas={c}&fechaEntrega={f}", csv, fecha.toString())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<PedidoDto>>() {
                    }));
        }

        public PedidoDto obtener(Long pedidoId) {
            return Remoto.ejecutar("ms-pedidos", () -> http.get().uri("/api/v1/pedidos/{id}", pedidoId)
                    .retrieve().body(PedidoDto.class));
        }

        /** Guarda el repartidor en el pedido (valida el estado en ms-pedidos). */
        public PedidoDto asignarRepartidor(Long pedidoId, Long repartidorId, String repartidorNombre, Long usuarioId) {
            Map<String, Object> cuerpo = new java.util.HashMap<>();
            cuerpo.put("repartidorId", repartidorId);
            cuerpo.put("repartidorNombre", repartidorNombre);
            cuerpo.put("usuarioId", usuarioId);
            return Remoto.ejecutar("ms-pedidos", () -> http.patch().uri("/api/v1/pedidos/{id}/repartidor", pedidoId)
                    .body(cuerpo).retrieve().body(PedidoDto.class));
        }

        public void marcarEnviado(Long pedidoId, Long usuarioId) {
            Remoto.intentar("ms-pedidos", () -> http.patch().uri("/api/v1/pedidos/{id}/estado", pedidoId)
                    .body(Map.of("estado", "ENVIADO", "nota", "Salió a ruta", "usuarioId", usuarioId == null ? 0L : usuarioId))
                    .retrieve().toBodilessEntity());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PedidoDto(Long id, String codigo, String nombreCliente, String telefono, String direccion,
            String comuna, String referencia, String horario, String estado, String metodoPago, BigDecimal total,
            LocalDate fechaEntrega, Long repartidorId) {
    }

    @Component
    public static class RepartoClient {
        private final RestClient http;

        public RepartoClient(RestClient.Builder builder, @Value("${bocado.servicios.reparto}") String url) {
            this.http = builder.baseUrl(url).build();
        }

        public String nombre(Long repartidorId) {
            try {
                RepartidorDto r = http.get().uri("/api/v1/repartidores/{id}", repartidorId).retrieve().body(RepartidorDto.class);
                return r == null ? null : r.nombre();
            } catch (Exception e) {
                return null;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RepartidorDto(Long id, String nombre) {
    }
}
