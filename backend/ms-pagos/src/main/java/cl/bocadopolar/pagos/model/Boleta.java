package cl.bocadopolar.pagos.model;

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

/** Boleta electrónica simulada: total con IVA incluido, desglosado en neto + IVA. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boletas")
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String numero;

    @Column(nullable = false, unique = true)
    private Long pagoId;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal neto;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal iva;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaIva;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal total;

    @Column(nullable = false)
    private Instant emision;
}
