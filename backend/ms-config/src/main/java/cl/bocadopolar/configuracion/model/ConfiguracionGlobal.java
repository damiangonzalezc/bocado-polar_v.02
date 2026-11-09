package cl.bocadopolar.configuracion.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Parámetros globales de la tienda (una sola fila). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "configuracion_global")
public class ConfiguracionGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String pais;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false)
    private Boolean preciosConIva;

    /** Porcentaje, por ejemplo 19.00 */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal iva;

    private LocalDate vigenteDesde;
}
