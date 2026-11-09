package cl.bocadopolar.marketing.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "carritos_abandonados")
public class CarritoAbandonado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Carrito en ms-pedidos (null si se registró a mano). */
    @Column(unique = true)
    private Long carritoId;

    private Long clienteId;

    @Column(length = 80)
    private String cliente;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 300)
    private String resumen;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal total;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false)
    private Boolean correoEnviado = false;

    private Instant correoEnviadoEn;

    @Column(length = 16)
    private String cuponOfrecido;

    @Column(nullable = false)
    private Boolean recuperado = false;

    private Long pedidoRecuperadoId;
}
