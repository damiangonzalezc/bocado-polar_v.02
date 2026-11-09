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
@Table(name = "calificaciones_repartidor")
public class CalificacionRepartidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long entregaId;

    @Column(nullable = false)
    private Long repartidorId;

    private Long clienteId;

    @Column(nullable = false)
    private Integer estrellas;

    @Column(nullable = false)
    private Instant fecha;
}
