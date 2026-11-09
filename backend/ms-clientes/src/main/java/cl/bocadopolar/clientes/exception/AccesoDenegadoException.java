package cl.bocadopolar.clientes.exception;

/** 403: el usuario no tiene permiso para esta acción. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
