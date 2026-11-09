package cl.bocadopolar.pedidos.model;

import java.time.Instant;

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

/** Cada cambio de estado queda registrado. Un retroceso se guarda como una entrada con esReversion=true. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPedido estado;

    @Column(length = 300)
    private String nota;

    private Long usuarioId;

    @Column(nullable = false)
    private Instant fecha;

    @Column(nullable = false)
    private Boolean esReversion;
}
