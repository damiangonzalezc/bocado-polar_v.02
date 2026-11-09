package cl.bocadopolar.configuracion.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Comuna con despacho y su tarifa de envío en CLP. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zonas_envio")
public class ZonaEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String comuna;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal tarifa;

    @Column(nullable = false)
    private Boolean activa;
}
