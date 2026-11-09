package cl.bocadopolar.reparto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado de visitar un pedido: entregado (con evidencia) o fallido. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entregas")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long repartidorId;

    private Long turnoId;

    @Column(nullable = false)
    private Long pedidoId;

    /** Parada en ms-rutas (opcional). */
    private Long paradaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoEntrega estado;

    @Column(length = 80)
    private String recibidoPor;

    private Double tempEntrega;

    @Column(length = 300)
    private String comentario;

    @Column(nullable = false)
    private Instant fecha;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "evidencia_id")
    private EvidenciaEntrega evidencia;
}
