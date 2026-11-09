package cl.bocadopolar.pedidos.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código visible para el cliente: BP-100001 */
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false, length = 80)
    private String nombreCliente;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String direccion;

    @Column(nullable = false, length = 60)
    private String comuna;

    @Column(length = 120)
    private String referencia;

    /** Comentario de entrega del cliente. */
    @Column(length = 300)
    private String comentario;

    @Column(nullable = false)
    private LocalDate fechaEntrega;

    @Column(nullable = false, length = 20)
    private String horario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MetodoPago metodoPago;

    /** Id del pago en ms-pagos (cuando existe). */
    private Long pagoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPedido estado;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal envio = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 16)
    private String codigoCupon;

    /** Repartidor asignado por el administrador (id de usuario en ms-auth). */
    private Long repartidorId;

    @Column(length = 80)
    private String repartidorNombre;

    private Instant asignadoEn;

    @Column(nullable = false)
    private Instant creadoEn;

    private Instant actualizadoEn;

    @Version
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id")
    @OrderBy("id ASC")
    private List<ItemPedido> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id")
    @OrderBy("fecha ASC, id ASC")
    private List<HistorialEstado> historial = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id")
    @OrderBy("fecha DESC")
    private List<NotaInterna> notas = new ArrayList<>();

    public BigDecimal getTotalKg() {
        return items.stream().map(ItemPedido::getKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void recalcularTotal() {
        subtotal = items.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal t = subtotal.add(envio).subtract(descuento);
        total = t.signum() < 0 ? BigDecimal.ZERO : t;
    }
}
