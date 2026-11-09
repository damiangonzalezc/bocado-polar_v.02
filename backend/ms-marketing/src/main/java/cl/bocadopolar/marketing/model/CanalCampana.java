package cl.bocadopolar.marketing.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Canal de origen de las visitas (Directo, Instagram, Google...) con su conversión. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "canales_campana")
public class CanalCampana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String canal;

    @Column(nullable = false)
    private Integer visitas;

    @Column(nullable = false)
    private Integer pedidos;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal ingresos;

    /** Pedidos / visitas en %. */
    public BigDecimal getConversion() {
        if (visitas == null || visitas == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(pedidos * 100L).divide(BigDecimal.valueOf(visitas), 2, RoundingMode.HALF_UP);
    }
}
