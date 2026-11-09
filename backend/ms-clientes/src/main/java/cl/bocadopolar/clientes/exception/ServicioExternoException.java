package cl.bocadopolar.clientes.exception;

/** 503: otro microservicio no respondió. */
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String mensaje) {
        super(mensaje);
    }
}
