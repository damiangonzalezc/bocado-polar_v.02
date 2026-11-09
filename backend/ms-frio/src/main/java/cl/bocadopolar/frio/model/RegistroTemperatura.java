package cl.bocadopolar.frio.model;

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

/**
 * Medición de temperatura. referenciaId depende del origen: LOTE → id del lote (ms-inventario),
 * SALIDA_RUTA → id del turno (ms-reparto), ENTREGA → id del pedido (ms-pedidos).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "registros_temperatura")
public class RegistroTemperatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private OrigenTemp origen;

    @Column(nullable = false)
    private Long referenciaId;

    @Column(nullable = false)
    private Double grados;

    @Column(nullable = false)
    private Instant fecha;

    private Long registradoPorId;

    public boolean excedeUmbral(double umbral) {
        return grados > umbral;
    }
}
