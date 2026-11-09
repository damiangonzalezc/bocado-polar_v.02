package cl.bocadopolar.inventario.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Verifica en ms-catalogo que el producto exista (dependencia "producto"). */
@Component
public class CatalogoClient {

    private final RestClient http;

    public CatalogoClient(RestClient.Builder builder, @Value("${bocado.servicios.catalogo}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public ProductoDto producto(Long id) {
        return Remoto.ejecutar("ms-catalogo",
                () -> http.get().uri("/api/v1/productos/{id}", id).retrieve().body(ProductoDto.class));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductoDto(Long id, String sku, String nombre, Boolean activo) {
    }
}
