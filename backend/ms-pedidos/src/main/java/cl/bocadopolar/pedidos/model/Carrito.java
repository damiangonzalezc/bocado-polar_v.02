package cl.bocadopolar.pedidos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Carrito en kg. Puede ser de un cliente (clienteId) o de un visitante (sesionAnonimaId). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "carritos")
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long clienteId;

    @Column(unique = true, length = 36)
    private String sesionAnonimaId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "carrito_id")
    @OrderBy("id ASC")
    private List<ItemCarrito> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalKg = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotal = BigDecimal.ZERO;

    private Instant creadoEn;

    private Instant actualizadoEn;

    public void recalcular() {
        totalKg = items.stream().map(ItemCarrito::getKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = items.stream().map(ItemCarrito::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        actualizadoEn = Instant.now();
    }
}
