package cl.bocadopolar.inventario.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "alertas_stock")
public class AlertaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productoId;

    @Column(length = 60)
    private String productoNombre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stockActual;

    @Column(nullable = false)
    private Boolean atendida;

    @Column(nullable = false)
    private Instant fecha;
}
