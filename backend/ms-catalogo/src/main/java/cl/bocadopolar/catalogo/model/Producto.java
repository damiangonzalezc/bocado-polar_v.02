package cl.bocadopolar.catalogo.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Producto del catálogo. El precio es por kilo (CLP, IVA incluido). El stock vive en ms-inventario. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String sku;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(length = 60)
    private String origen;

    @Column(length = 100)
    private String alergenos;

    /** Ruta o URL de la imagen (ej: img/prod-salmon.svg). */
    @Column(length = 255)
    private String imagen;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal precioKg;

    @Column(nullable = false, precision = 10, scale = 0)
    private BigDecimal costoKg;

    @Column(nullable = false)
    private Double valoracionProm = 0.0;

    @Column(nullable = false)
    private Integer valoracionVotos = 0;

    /** Ej: {"Marca":"BOCADO Polar","Formato":"Caja 5 kg","Calibre":"Grande"} */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "producto_atributos", joinColumns = @JoinColumn(name = "producto_id"))
    @MapKeyColumn(name = "atributo", length = 30)
    @Column(name = "valor", length = 40)
    private Map<String, String> atributos = new LinkedHashMap<>();

    @Version
    private Long version;

    /** Margen bruto en % sobre el precio de venta (solo informativo para el panel). */
    public BigDecimal getMargen() {
        if (precioKg == null || costoKg == null || precioKg.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return precioKg.subtract(costoKg).multiply(BigDecimal.valueOf(100)).divide(precioKg, 1, RoundingMode.HALF_UP);
    }

    /** Promedio redondeado a media estrella, como lo muestra el catálogo. */
    public Double getValoracion() {
        return Math.round(valoracionProm * 2) / 2.0;
    }
}
