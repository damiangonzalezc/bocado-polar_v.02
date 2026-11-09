package cl.bocadopolar.reparto.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ficha del repartidor. Su id es el MISMO id de usuario de ms-auth (así repartidorId significa lo mismo en todo el sistema). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "repartidores")
public class Repartidor {

    @Id
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 120)
    private String email;

    @Column(length = 12)
    private String rut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Disponibilidad disponibilidad;

    @Column(nullable = false)
    private Boolean sonido;

    @Column(nullable = false)
    private Boolean vibracion;

    @Column(nullable = false)
    private Double calificacionProm;

    @Column(nullable = false)
    private Integer votos;
}
