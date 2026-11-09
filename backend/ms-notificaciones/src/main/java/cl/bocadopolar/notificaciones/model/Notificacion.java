package cl.bocadopolar.notificaciones.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notificaciones")
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id del evento que envió el otro microservicio: evita duplicados si lo reintenta. */
    @Column(unique = true, length = 64)
    private String eventoId;

    @Column(nullable = false, length = 120)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MotivoNotificacion motivo;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    @Column(nullable = false)
    private Integer intentos = 0;

    @Column(nullable = false)
    private Integer maxIntentos = 3;

    private Instant proximoReintento;

    private Instant enviadaEn;

    @Column(nullable = false)
    private Instant creadaEn;

    /** Para los avisos del panel admin (campana de notificaciones). */
    @Column(nullable = false)
    private Boolean leida = false;

    @Column(length = 300)
    private String ultimoError;

    public boolean puedeReintentar() {
        return estado == EstadoEnvio.FALLIDA && intentos < maxIntentos;
    }
}
