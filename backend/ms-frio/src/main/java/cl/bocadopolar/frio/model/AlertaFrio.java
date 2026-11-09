package cl.bocadopolar.frio.model;

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
@Table(name = "alertas_frio")
public class AlertaFrio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long registroId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrigenTemp origen;

    @Column(nullable = false)
    private Long referenciaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Severidad severidad;

    @Column(nullable = false, length = 300)
    private String detalle;

    @Column(nullable = false)
    private Integer minutosSobreUmbral;

    @Column(nullable = false)
    private Boolean atendida;

    private Long atendidaPorId;

    @Column(nullable = false)
    private Instant fecha;
}
