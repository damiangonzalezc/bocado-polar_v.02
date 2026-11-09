package cl.bocadopolar.clientes.model;

import java.math.BigDecimal;
import java.time.Instant;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ficha CRM del cliente. usuarioId enlaza con ms-auth (puede ser null en clientes B2B creados por ventas). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long usuarioId;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 40)
    private String apellido;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(length = 12)
    private String rut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SegmentoCliente segmento = SegmentoCliente.NUEVO;

    @Column(nullable = false, precision = 14, scale = 0)
    private BigDecimal gastoTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer totalPedidos = 0;

    private Instant creadoEn;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cliente_id")
    @OrderBy("id ASC")
    private List<DireccionEntrega> direcciones = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cliente_id")
    @OrderBy("id ASC")
    private List<MedioPago> mediosPago = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cliente_id")
    @OrderBy("fecha DESC")
    private List<NotaCliente> notas = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "preferencia_id")
    private PreferenciaCliente preferencia = new PreferenciaCliente();

    public String nombreCompleto() {
        return apellido == null || apellido.isBlank() ? nombre : nombre + " " + apellido;
    }
}
