package cl.bocadopolar.marketing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import cl.bocadopolar.marketing.model.TipoDescuento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MarketingDtos {

    private MarketingDtos() {
    }

    public record CuponRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{4,16}$", message = "4 a 16 letras o números, sin espacios") String codigo,
            @NotNull TipoDescuento tipo,
            @NotNull @DecimalMin("1") BigDecimal valor,
            @DecimalMin("0") BigDecimal compraMinima,
            @NotNull @Min(1) Integer limiteUsos,
            @Min(1) Integer limitePorCliente,
            Boolean activo,
            @NotNull LocalDate vence) {
    }

    public record ActivoRequest(@NotNull Boolean activo) {
    }

    public record ValidarCuponRequest(@NotBlank String codigo, Long clienteId, @NotNull @DecimalMin("0") BigDecimal subtotal) {
    }

    public record ValidacionCupon(boolean valido, String codigo, BigDecimal descuento, String mensaje) {
    }

    public record UsoRequest(@NotBlank String codigo, @NotNull Long pedidoId, Long clienteId) {
    }

    public record CanalRequest(@NotBlank @Size(max = 40) String canal, @Min(0) Integer visitas, @Min(0) Integer pedidos,
            @DecimalMin("0") BigDecimal ingresos) {
    }

    public record VisitaCanalRequest(@NotBlank @Size(max = 40) String canal) {
    }

    public record ConversionCanalRequest(@NotBlank @Size(max = 40) String canal, @NotNull @DecimalMin("0") BigDecimal monto) {
    }

    public record AbandonadoRequest(@Size(max = 80) String cliente, @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 300) String resumen, @NotNull @DecimalMin("0") BigDecimal total) {
    }

    public record RescateRequest(@Pattern(regexp = "^$|^[A-Za-z0-9]{4,16}$") String codigoCupon) {
    }

    public record RecuperadoRequest(Long pedidoId) {
    }
}
