package cl.bocadopolar.frio.model;

import java.time.Duration;
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

/** Cuenta regresiva de 90 minutos desde que el repartidor sale de bodega (RUTA_MAX_MIN). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cuentas_regresivas")
public class CuentaRegresivaRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long turnoId;

    @Column(nullable = false)
    private Instant inicio;

    @Column(nullable = false)
    private Integer limiteMin;

    @Column(nullable = false)
    private Integer umbralAvisoMin;

    public long getMinutosRestantes() {
        return limiteMin - Duration.between(inicio, Instant.now()).toMinutes();
    }

    public boolean isDebeAvisar() {
        long r = getMinutosRestantes();
        return r <= umbralAvisoMin && r > 0;
    }

    public boolean isExcedida() {
        return getMinutosRestantes() <= 0;
    }
}
