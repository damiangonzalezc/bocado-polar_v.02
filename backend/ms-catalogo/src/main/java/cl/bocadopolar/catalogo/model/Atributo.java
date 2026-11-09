package cl.bocadopolar.catalogo.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Atributo de producto (Calibre, Marca, Formato) con sus valores posibles. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "atributos")
public class Atributo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String nombre;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "atributo_valores", joinColumns = @JoinColumn(name = "atributo_id"))
    @Column(name = "valor", length = 40)
    private List<String> valores = new ArrayList<>();

    public Atributo(String nombre, List<String> valores) {
        this.nombre = nombre;
        this.valores = new ArrayList<>(valores);
    }
}
