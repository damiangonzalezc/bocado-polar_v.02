package cl.bocadopolar.configuracion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Regla de negocio parametrizable (RN-01 temperatura, RN-02 kg por formato, stock crítico, etc.). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reglas_negocio")
public class ReglaNegocio {

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(length = 20)
    private String unidad;

    private LocalDate vigenteDesde;
}
