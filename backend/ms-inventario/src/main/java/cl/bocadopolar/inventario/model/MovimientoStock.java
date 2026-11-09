package cl.bocadopolar.inventario.model;

import java.math.BigDecimal;
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

/** Historial de entradas, salidas, ajustes y mermas. kg es positivo para entradas y negativo para salidas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "movimientos_stock")
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loteId;

    @Column(nullable = false)
    private Long productoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal kg;

    @Column(nullable = false)
    private Instant fecha;

    private Long usuarioId;
}
