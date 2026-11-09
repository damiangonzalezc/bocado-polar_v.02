package cl.bocadopolar.reportes.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permite que el front (React + Vite en http://localhost:5173) llame a este
 * microservicio directamente. Cuando el front pasa por el API Gateway (puerto 8080)
 * es el gateway quien responde el CORS y elimina los encabezados duplicados.
 */
@Configuration
public class CorsConfig {

    @Value("${bocado.cors.origenes}")
    private String[] origenes;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(origenes)
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Location")
                        .allowCredentials(false)
                        .maxAge(3600);
            }
        };
    }
}
