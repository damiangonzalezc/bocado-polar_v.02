package cl.bocadopolar.pagos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bocadoOpenApi() {
        return new OpenAPI().info(new Info()
                .title("BOCADO Polar · ms-pagos")
                .description("Pagos y Boletas")
                .version("v1"));
    }
}
