package cl.bocadopolar.marketing.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cupones")
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoDescuento tipo;

    /** % si es PORCENTAJE, CLP si es MONTO_FIJO. */
    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal valor;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal compraMinima = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer limiteUsos;

    @Column(nullable = false)
    private Integer usos = 0;

    /** Veces que un mismo cliente puede usarlo. */
    @Column(nullable = false)
    private Integer limitePorCliente = 1;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false)
    private LocalDate vence;

    @Version
    private Long version;

    public boolean estaVigente() {
        return Boolean.TRUE.equals(activo) && !vence.isBefore(LocalDate.now()) && usos < limiteUsos;
    }

    public BigDecimal calcularDescuento(BigDecimal subtotal) {
        BigDecimal d = tipo == TipoDescuento.PORCENTAJE
                ? subtotal.multiply(valor).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : valor;
        return d.min(subtotal);
    }
}
