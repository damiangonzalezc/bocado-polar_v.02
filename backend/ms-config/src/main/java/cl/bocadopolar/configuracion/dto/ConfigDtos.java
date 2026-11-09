package cl.bocadopolar.configuracion.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpos de entrada y salida de ms-config. */
public final class ConfigDtos {

    private ConfigDtos() {
    }

    public record ConfiguracionRequest(
            @NotBlank @Size(max = 60) String pais,
            @NotBlank @Size(min = 3, max = 3) String moneda,
            @NotNull Boolean preciosConIva,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal iva) {
    }

    public record ZonaRequest(
            @NotBlank @Size(min = 3, max = 60) String comuna,
            @NotNull @DecimalMin("0") BigDecimal tarifa,
            Boolean activa) {
    }

    public record EmpresaRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String tipo,
            Boolean activa) {
    }

    public record ActivaRequest(@NotNull Boolean activa) {
    }

    public record ReglaRequest(
            @NotBlank @Pattern(regexp = "^[A-Z0-9_]{3,40}$", message = "Usa MAYÚSCULAS, números y guion bajo") String codigo,
            @NotBlank @Size(max = 200) String descripcion,
            @NotNull BigDecimal valor,
            @Size(max = 20) String unidad) {
    }

    public record ReglaValorRequest(@NotNull BigDecimal valor, @Size(max = 200) String descripcion,
            @Size(max = 20) String unidad) {
    }

    public record TarifaResponse(String comuna, BigDecimal tarifa) {
    }

    public record IvaResponse(BigDecimal iva) {
    }

    public record PasarelaDisponibleResponse(String nombre, boolean conectada) {
    }
}
