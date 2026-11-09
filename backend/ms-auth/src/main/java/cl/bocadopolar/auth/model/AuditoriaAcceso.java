package cl.bocadopolar.auth.model;

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
@Table(name = "auditoria_accesos")
public class AuditoriaAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long usuarioId;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 60)
    private String accion;

    @Column(nullable = false)
    private Boolean exitoso;

    @Column(nullable = false)
    private Instant fecha;

    @Column(length = 45)
    private String ip;
}
