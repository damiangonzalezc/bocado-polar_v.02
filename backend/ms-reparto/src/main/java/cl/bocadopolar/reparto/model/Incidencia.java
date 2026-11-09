package cl.bocadopolar.reparto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 22)
    private TipoIncidencia tipo;

    @Column(length = 300)
    private String nota;

    private Long entregaId;

    private Long pedidoId;

    @Column(nullable = false)
    private Long reportadaPorId;

    @Column(nullable = false)
    private Instant fecha;
}
