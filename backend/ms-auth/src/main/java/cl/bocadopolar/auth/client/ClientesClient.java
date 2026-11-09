package cl.bocadopolar.auth.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.bocadopolar.auth.model.Usuario;

/** Llama a ms-clientes para crear el perfil del cliente al registrarse (dependencia "perfil"). */
@Component
public class ClientesClient {

    private final RestClient http;

    public ClientesClient(RestClient.Builder builder, @Value("${bocado.servicios.clientes}") String url) {
        this.http = builder.baseUrl(url).build();
    }

    public void crearPerfil(Usuario u) {
        Remoto.intentar("ms-clientes", () -> http.post()
                .uri("/api/v1/clientes/desde-usuario")
                .body(Map.of(
                        "usuarioId", u.getId(),
                        "nombre", u.getNombre(),
                        "apellido", u.getApellido(),
                        "email", u.getEmail(),
                        "telefono", u.getTelefono() == null ? "" : u.getTelefono(),
                        "rut", u.getRut() == null ? "" : u.getRut()))
                .retrieve()
                .toBodilessEntity());
    }
}
