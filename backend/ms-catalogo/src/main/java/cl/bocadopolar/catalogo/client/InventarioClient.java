package cl.bocadopolar.catalogo.client;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Consulta a ms-inventario si un producto todavía tiene kilos en bodega. */
@Component
public class InventarioClient {

    private final RestClient http;

    public InventarioClient(RestClient.Builder builder, @Value("${bocado.servicios.inventario}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    /** Devuelve el stock en kg, o null si ms-inventario no responde. */
    public BigDecimal stockKg(Long productoId) {
        try {
            StockDto s = http.get().uri("/api/v1/stock/{id}", productoId).retrieve().body(StockDto.class);
            return s == null ? null : s.stockKg();
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StockDto(Long productoId, BigDecimal stockKg) {
    }
}
