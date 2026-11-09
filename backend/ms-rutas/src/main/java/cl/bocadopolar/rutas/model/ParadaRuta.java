package cl.bocadopolar.rutas.model;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "paradas_ruta")
public class ParadaRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(length = 20)
    private String codigoPedido;

    @Column(length = 80)
    private String cliente;

    @Column(length = 20)
    private String telefono;

    @Column(nullable = false, length = 100)
    private String direccion;

    @Column(nullable = false, length = 60)
    private String comuna;

    @Column(length = 120)
    private String referencia;

    @Column(length = 20)
    private String horario;

    /** Si el pedido se paga al recibir, cuánto debe cobrar el repartidor. */
    @Column(nullable = false)
    private Boolean cobrarAlEntregar = false;

    @Column(precision = 12, scale = 0)
    private BigDecimal montoACobrar;

    @Column(nullable = false)
    private Integer orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoParada estado = EstadoParada.PENDIENTE;

    @Column(length = 200)
    private String motivoFalla;

    private Instant cerradaEn;
}
