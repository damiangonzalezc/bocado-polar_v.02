package cl.bocadopolar.clientes.client;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Lee reglas de negocio desde ms-config; si no responde usa el valor por defecto. */
@Component
public class ConfigClient {

    private final RestClient http;

    public ConfigClient(RestClient.Builder builder, @Value("${bocado.servicios.config}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public BigDecimal regla(String codigo, BigDecimal porDefecto) {
        try {
            ReglaDto r = http.get().uri("/api/v1/reglas/{codigo}", codigo).retrieve().body(ReglaDto.class);
            return r != null && r.valor() != null ? r.valor() : porDefecto;
        } catch (Exception e) {
            return porDefecto;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReglaDto(String codigo, BigDecimal valor) {
    }
}
