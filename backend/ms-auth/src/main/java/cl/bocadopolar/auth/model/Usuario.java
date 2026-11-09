package cl.bocadopolar.auth.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String apellido;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Nunca se envía al front. */
    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 12)
    private String rut;

    @Column(length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Rol rol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoUsuario estado;

    /** El administrador original no se puede bloquear, suspender, eliminar ni cambiar de rol. */
    @Column(nullable = false)
    private Boolean protegido;

    private Instant creadoEn;

    public String nombreCompleto() {
        return nombre + " " + apellido;
    }
}
