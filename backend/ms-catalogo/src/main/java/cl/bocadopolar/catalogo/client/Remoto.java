package cl.bocadopolar.catalogo.client;

import java.util.function.Supplier;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.bocadopolar.catalogo.exception.RecursoNoEncontradoException;
import cl.bocadopolar.catalogo.exception.ReglaNegocioException;
import cl.bocadopolar.catalogo.exception.ServicioExternoException;
import cl.bocadopolar.catalogo.exception.SolicitudInvalidaException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utilidad para llamar a otros microservicios por REST.
 * - {@link #ejecutar}: llamada obligatoria; si falla, el error se traduce a nuestras excepciones.
 * - {@link #intentar}: llamada "de mejor esfuerzo" (avisos, estadísticas); si falla solo se registra en el log.
 */
@Slf4j
public final class Remoto {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Remoto() {
    }

    public static <T> T ejecutar(String servicio, Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String mensaje = mensaje(e, servicio + " respondió con estado " + status);
            if (status == 404) {
                throw new RecursoNoEncontradoException(mensaje);
            }
            if (status == 400) {
                throw new SolicitudInvalidaException(mensaje);
            }
            if (status == 409 || status == 422) {
                throw new ReglaNegocioException(mensaje);
            }
            throw new ServicioExternoException(mensaje);
        } catch (ResourceAccessException e) {
            throw new ServicioExternoException("No se pudo conectar con " + servicio + ". Verifica que esté levantado.");
        }
    }

    public static void intentar(String servicio, Runnable llamada) {
        try {
            llamada.run();
        } catch (Exception e) {
            log.warn("Llamada opcional a {} falló: {}", servicio, e.getMessage());
        }
    }

    private static String mensaje(RestClientResponseException e, String porDefecto) {
        try {
            JsonNode nodo = MAPPER.readTree(e.getResponseBodyAsString());
            if (nodo != null && nodo.hasNonNull("mensaje")) {
                return nodo.get("mensaje").asText();
            }
        } catch (Exception ignorada) {
            // el cuerpo no era JSON
        }
        return porDefecto;
    }
}
