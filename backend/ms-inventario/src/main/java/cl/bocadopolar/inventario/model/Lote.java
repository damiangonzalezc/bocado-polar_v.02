package cl.bocadopolar.inventario.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lote de un producto con stock en kg y vencimiento. La salida se hace por FEFO (primero el que vence antes). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lotes")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productoId;

    /** Copia del nombre en ms-catalogo para mostrarlo sin otra llamada. */
    @Column(length = 60)
    private String productoNombre;

    @Column(nullable = false, unique = true, length = 16)
    private String codigoLote;

    /** Temperatura de almacenaje registrada al ingreso (°C). */
    private Double tempAlmacenaje;

    @Column(nullable = false)
    private Boolean bloqueado = false;

    @Column(length = 200)
    private String motivoBloqueo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stockKg = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal reservadoKg = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate vencimiento;

    private Instant ingreso;

    @Version
    private Long version;

    public BigDecimal getDisponibleKg() {
        return stockKg.subtract(reservadoKg);
    }

    public Long getDiasParaVencer() {
        return ChronoUnit.DAYS.between(LocalDate.now(), vencimiento);
    }

    public boolean vencido() {
        return vencimiento.isBefore(LocalDate.now());
    }

    public boolean utilizable() {
        return !Boolean.TRUE.equals(bloqueado) && !vencido() && getDisponibleKg().signum() > 0;
    }
}
