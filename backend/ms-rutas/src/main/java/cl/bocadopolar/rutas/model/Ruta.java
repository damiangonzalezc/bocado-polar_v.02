package cl.bocadopolar.rutas.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ruta de despacho por comunas con repartidor, vehículo y hora de salida. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rutas")
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código visible: R-001 */
    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fecha;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ruta_comunas", joinColumns = @JoinColumn(name = "ruta_id"))
    @Column(name = "comuna", length = 60)
    private List<String> comunas = new ArrayList<>();

    @Column(nullable = false)
    private LocalTime horaSalida;

    /** Id de usuario (ms-auth) del repartidor a cargo. */
    private Long repartidorId;

    @Column(length = 80)
    private String repartidorNombre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoRuta estado = EstadoRuta.PLANIFICADA;

    @Column(length = 300)
    private String notas;

    private Instant iniciadaEn;

    private Instant finalizadaEn;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ruta_id")
    @OrderBy("orden ASC")
    private List<ParadaRuta> paradas = new ArrayList<>();

    public long getParadasPendientes() {
        return paradas.stream().filter(p -> p.getEstado() == EstadoParada.PENDIENTE).count();
    }
}
