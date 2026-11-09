package cl.bocadopolar.catalogo.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record ProductoRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9-]{3,12}$", message = "3 a 12 MAYÚSCULAS, números o guiones (ej: SAL-001)") String sku,
            @NotBlank @Size(min = 3, max = 60) String nombre,
            @Size(max = 500) String descripcion,
            @NotNull Long categoriaId,
            @Size(max = 60) String origen,
            @Size(max = 100) String alergenos,
            @Size(max = 255) String imagen,
            Boolean activo,
            @NotNull @DecimalMin(value = "1", message = "El precio debe ser mayor a 0") BigDecimal precioKg,
            @NotNull @DecimalMin("0") BigDecimal costoKg,
            Map<String, String> atributos) {
    }

    public record ActivoRequest(@NotNull Boolean activo) {
    }

    /** Ajuste masivo: ids vacío = todos los productos. Porcentaje positivo sube, negativo baja. */
    public record AjustePreciosRequest(List<Long> ids,
            @NotNull @DecimalMin("-90") @DecimalMax("500") BigDecimal porcentaje) {
    }

    public record CategoriaRequest(@NotBlank @Size(min = 3, max = 40) String nombre, @Size(max = 200) String descripcion) {
    }

    public record AtributoRequest(@NotBlank @Size(min = 2, max = 30) String nombre, List<@NotBlank @Size(max = 40) String> valores) {
    }

    public record ValorRequest(@NotBlank @Size(max = 40) String valor) {
    }

    public record ValoracionRequest(@NotNull Long clienteId,
            @NotNull @DecimalMin("0.5") @DecimalMax("5.0") Double estrellas) {
    }

    /** prom = promedio redondeado a media estrella; mia = voto del cliente consultado (0 si no votó). */
    public record ResumenValoracion(Long productoId, Double promedio, Integer votos, Double mia) {
    }
}
