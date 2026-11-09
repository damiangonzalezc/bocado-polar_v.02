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

/** Cobro que hace el repartidor al entregar (efectivo con vuelto, POS móvil o transferencia). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cobros_contra_entrega")
public class CobroContraEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long pagoId;

    @Column(nullable = false)
    private Long pedidoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoCobro tipo;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal monto;

    @Column(nullable = false)
    private Boolean cobrado = false;

    /** Con cuánto paga el cliente (solo efectivo). */
    @Column(precision = 12, scale = 0)
    private BigDecimal pagaCon;

    @Column(precision = 12, scale = 0)
    private BigDecimal vuelto;

    /** Id de usuario del repartidor que cobró. */
    private Long cobradoPorId;

    private Instant fechaCobro;

    public BigDecimal calcularVuelto() {
        if (tipo != TipoCobro.EFECTIVO || pagaCon == null) {
            return BigDecimal.ZERO;
        }
        return pagaCon.subtract(monto).max(BigDecimal.ZERO);
    }
}
