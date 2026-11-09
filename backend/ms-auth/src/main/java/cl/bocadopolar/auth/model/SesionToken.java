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

/** Cada inicio de sesión emite un JWT; aquí se guarda su identificador (jti) para poder revocarlo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sesiones")
public class SesionToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false, unique = true, length = 36)
    private String jti;

    @Column(nullable = false)
    private Instant creada;

    @Column(nullable = false)
    private Instant expira;

    @Column(nullable = false)
    private Boolean revocado;

    public boolean activa() {
        return !Boolean.TRUE.equals(revocado) && expira.isAfter(Instant.now());
    }

    public void revocar() {
        this.revocado = true;
    }
}
