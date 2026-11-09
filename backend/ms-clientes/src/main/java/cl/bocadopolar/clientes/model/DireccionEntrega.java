package cl.bocadopolar.clientes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "direcciones_entrega")
public class DireccionEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String alias;

    @Column(nullable = false, length = 100)
    private String calle;

    @Column(nullable = false, length = 60)
    private String comuna;

    @Column(length = 120)
    private String referencia;

    @Column(nullable = false)
    private Boolean principal;
}
