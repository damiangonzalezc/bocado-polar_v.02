package cl.bocadopolar.inventario.exception;

/** 400: los datos enviados no son válidos. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
