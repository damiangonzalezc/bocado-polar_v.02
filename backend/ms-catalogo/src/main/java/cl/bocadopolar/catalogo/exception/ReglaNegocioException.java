package cl.bocadopolar.catalogo.exception;

/** 409: la operación viola una regla de negocio o el estado actual. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
