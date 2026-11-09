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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reembolsos")
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pagoId;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoReembolso estado;

    private Long solicitadoPorId;

    private Long aprobadoPorId;

    @Column(length = 300)
    private String motivoRechazo;

    @Column(nullable = false)
    private Instant fecha;
}
