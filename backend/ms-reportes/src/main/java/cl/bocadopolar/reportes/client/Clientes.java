package cl.bocadopolar.reportes.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** ms-reportes solo lee: pedidos (ventas), inventario (stock), frío, reparto (incidencias), config (IVA). */
@Component
public class Clientes {

    private final RestClient pedidos;
    private final RestClient inventario;
    private final RestClient frio;
    private final RestClient reparto;
    private final RestClient config;
    private final RestClient marketing;

    public Clientes(RestClient.Builder builder,
            @Value("${bocado.servicios.pedidos}") String urlPedidos,
            @Value("${bocado.servicios.inventario}") String urlInventario,
            @Value("${bocado.servicios.frio}") String urlFrio,
            @Value("${bocado.servicios.reparto}") String urlReparto,
            @Value("${bocado.servicios.config}") String urlConfig,
            @Value("${bocado.servicios.marketing}") String urlMarketing) {
        this.pedidos = builder.clone().baseUrl(urlPedidos).build();
        this.inventario = builder.clone().baseUrl(urlInventario).build();
        this.frio = builder.clone().baseUrl(urlFrio).build();
        this.reparto = builder.clone().baseUrl(urlReparto).build();
        this.config = builder.clone().baseUrl(urlConfig).build();
        this.marketing = builder.clone().baseUrl(urlMarketing).build();
    }

    public List<PedidoDto> pedidos(LocalDate desde, LocalDate hasta) {
        return Remoto.ejecutar("ms-pedidos", () -> pedidos.get()
                .uri("/api/v1/pedidos?desde={d}&hasta={h}", desde.toString(), hasta.toString())
                .retrieve().body(new ParameterizedTypeReference<List<PedidoDto>>() {
                }));
    }

    public BigDecimal iva() {
        try {
            IvaDto d = config.get().uri("/api/v1/configuracion/iva").retrieve().body(IvaDto.class);
            return d == null || d.iva() == null ? new BigDecimal("19") : d.iva();
        } catch (Exception e) {
            return new BigDecimal("19");
        }
    }

    /** Cantidad de elementos que devuelve una lista; -1 si el servicio no responde. */
    public int contarAlertasStock() {
        return contar(inventario, "/api/v1/alertas-stock?atendida=false");
    }

    public int contarAlertasFrio() {
        return contar(frio, "/api/v1/alertas-frio?atendida=false");
    }

    public List<IncidenciaDto> incidencias() {
        try {
            List<IncidenciaDto> l = reparto.get().uri("/api/v1/incidencias").retrieve()
                    .body(new ParameterizedTypeReference<List<IncidenciaDto>>() {
                    });
            return l == null ? List.of() : l;
        } catch (Exception e) {
            return List.of();
        }
    }

    public void visitaCanal(String canal) {
        Remoto.intentar("ms-marketing", () -> marketing.post().uri("/api/v1/canales/visita")
                .body(Map.of("canal", canal)).retrieve().toBodilessEntity());
    }

    private int contar(RestClient http, String uri) {
        try {
            List<Object> l = http.get().uri(uri).retrieve().body(new ParameterizedTypeReference<List<Object>>() {
            });
            return l == null ? 0 : l.size();
        } catch (Exception e) {
            return -1;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PedidoDto(Long id, String estado, BigDecimal total, Instant creadoEn, LocalDate fechaEntrega,
            List<ItemDto> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ItemDto(Long productoId, String nombre, BigDecimal kg, BigDecimal subtotal) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IvaDto(BigDecimal iva) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IncidenciaDto(Long id, String tipo, Instant fecha) {
    }
}
