package cl.bocadopolar.pedidos.model;

/** Orden del flujo normal: PENDIENTE_PAGO → PAGADO → EN_PREPARACION → ENVIADO → ENTREGADO. */
public enum EstadoPedido {
    PENDIENTE_PAGO, PAGADO, EN_PREPARACION, ENVIADO, ENTREGADO, CANCELADO, REEMBOLSADO
}
