package cl.bocadopolar.catalogo.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un voto por cliente y producto (si vuelve a votar, se actualiza). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "valoraciones", uniqueConstraints = @UniqueConstraint(columnNames = { "producto_id", "cliente_id" }))
public class Valoracion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Double estrellas;

    @Column(nullable = false)
    private Instant fecha;
}
