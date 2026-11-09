package cl.bocadopolar.inventario.model;

import java.math.BigDecimal;
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

/** Kilos apartados de un lote para un pedido mientras se paga. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservas_stock")
public class ReservaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private Long loteId;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal kg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoReserva estado;

    @Column(nullable = false)
    private Instant creadaEn;

    @Column(nullable = false)
    private Instant expiraEn;
}
