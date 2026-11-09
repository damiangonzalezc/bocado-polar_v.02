package cl.bocadopolar.reparto.exception;

/** 400: los datos enviados no son válidos. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
