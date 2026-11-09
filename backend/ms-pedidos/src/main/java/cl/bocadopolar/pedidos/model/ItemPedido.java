package cl.bocadopolar.pedidos.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Línea del pedido: guarda nombre y precio del momento de la compra. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "items_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal precioKg;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal kg;

    public BigDecimal getSubtotal() {
        return precioKg.multiply(kg).setScale(0, RoundingMode.HALF_UP);
    }
}
