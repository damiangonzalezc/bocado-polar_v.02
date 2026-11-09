package cl.bocadopolar.reportes.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

/** Foto de los KPI de un período (se guarda al pedir un snapshot, para tener historial). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kpis_periodo")
public class KpiPeriodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Periodo periodo;

    @Column(nullable = false)
    private LocalDate desde;

    @Column(nullable = false)
    private LocalDate hasta;

    @Column(nullable = false)
    private Integer ordenes;

    @Column(nullable = false)
    private Integer visitas;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal conversion;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal ventasBrutas;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal ventasNetas;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal ticketPromedio;

    @Column(nullable = false)
    private Instant calculadoEn;
}
