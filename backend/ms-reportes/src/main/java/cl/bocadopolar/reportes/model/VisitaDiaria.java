package cl.bocadopolar.reportes.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Visitas al sitio por día (para la tasa de conversión). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visitas_diarias")
public class VisitaDiaria {

    @Id
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer visitas;
}
