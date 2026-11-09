package cl.bocadopolar.pedidos.model;

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

/** Nota del personal sobre el pedido (no la ve el cliente). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notas_internas")
public class NotaInterna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String texto;

    private Long autorId;

    @Column(nullable = false)
    private Instant fecha;
}
