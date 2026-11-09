package cl.bocadopolar.pagos.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(length = 20)
    private String codigoPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MetodoPago metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoPago estado;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal monto;

    /** Pasarela usada (Webpay, Transferencia bancaria) o "Contra entrega". */
    @Column(length = 40)
    private String pasarela;

    /** Código de autorización o referencia que entrega la pasarela. */
    @Column(length = 60)
    private String referencia;

    /** Evita cobrar dos veces si el front reintenta la misma solicitud. */
    @Column(unique = true, length = 64)
    private String idempotencyKey;

    /** Texto para mostrar al cliente (datos de transferencia, siguiente paso, etc.). */
    @Column(length = 300)
    private String instrucciones;

    @Column(length = 200)
    private String motivoRechazo;

    @Column(nullable = false)
    private Instant fecha;

    private Instant actualizadoEn;
}
