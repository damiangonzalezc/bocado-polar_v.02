package cl.bocadopolar.pagos.exception;

import java.time.Instant;
import java.util.Map;

/** Formato único de error que reciben todos los clientes (front y otros microservicios). */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> detalles) {
}
