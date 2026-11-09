package cl.bocadopolar.clientes.exception;

/** 404: el recurso pedido no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
