package cl.bocadopolar.reparto.exception;

/** 401: falta iniciar sesión o el token no es válido. */
public class NoAutorizadoException extends RuntimeException {

    public NoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
