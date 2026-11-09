package cl.bocadopolar.clientes.model;

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

/** Nota de soporte o ventas en la ficha CRM (reclamo, devolución, consulta...). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notas_cliente")
public class NotaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoNota tipo;

    @Column(nullable = false, length = 500)
    private String texto;

    private Long autorId;

    @Column(nullable = false)
    private Instant fecha;
}
