package cl.bocadopolar.reparto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Rendición de dinero al terminar el turno. diferencia = rendido - esperado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cierres_caja")
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long repartidorId;

    @Column(nullable = false, unique = true)
    private Long turnoId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer entregas;

    @Column(nullable = false)
    private Integer fallidas;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal efectivoEsperado;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal efectivoRendido;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal diferencia;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal pos;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal transferencia;

    private Instant creadoEn;
}
