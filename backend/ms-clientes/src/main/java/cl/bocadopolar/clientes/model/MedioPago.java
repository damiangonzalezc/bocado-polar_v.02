package cl.bocadopolar.clientes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Tarjeta guardada: solo alias, marca, últimos 4 dígitos y el token que entrega la pasarela. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medios_pago")
public class MedioPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String alias;

    @Column(nullable = false, length = 20)
    private String marca;

    @Column(nullable = false, length = 4)
    private String ultimos4;

    @JsonIgnore
    @Column(nullable = false, length = 120)
    private String tokenPasarela;

    /** Formato MM/AA */
    @Column(nullable = false, length = 5)
    private String vence;
}
