package cl.bocadopolar.reparto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Jornada del repartidor: desde que sale con la caja fría hasta el cierre de caja. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "turnos")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long repartidorId;

    /** Ruta de ms-rutas que está haciendo (si tenía una asignada hoy). */
    private Long rutaId;

    @Column(nullable = false)
    private Instant inicio;

    private Instant fin;

    @Column(nullable = false)
    private Double tempCajaInicial;

    /** true si la temperatura de salida superó el umbral RN-01. */
    @Column(nullable = false)
    private Boolean alertaTemperatura;

    public boolean activo() {
        return fin == null;
    }
}
