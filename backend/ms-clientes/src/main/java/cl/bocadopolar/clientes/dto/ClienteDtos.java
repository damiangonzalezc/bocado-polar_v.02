package cl.bocadopolar.clientes.dto;

import java.math.BigDecimal;

import cl.bocadopolar.clientes.model.TipoNota;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ClienteDtos {

    public static final String TELEFONO_REGEX = "^$|^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$";

    private ClienteDtos() {
    }

    /** Alta de ficha (desde ms-auth al registrarse, o manual desde el CRM). */
    public record ClienteRequest(
            Long usuarioId,
            @NotBlank @Size(max = 60) String nombre,
            @Size(max = 40) String apellido,
            @NotBlank @Email @Size(max = 120) String email,
            @Pattern(regexp = TELEFONO_REGEX, message = "Formato: +56 9 1234 5678") String telefono,
            @Size(max = 12) String rut) {
    }

    public record PerfilRequest(
            @NotBlank @Size(min = 2, max = 60) String nombre,
            @Size(max = 40) String apellido,
            @Pattern(regexp = TELEFONO_REGEX, message = "Formato: +56 9 1234 5678") String telefono,
            @Size(max = 12) String rut) {
    }

    public record DireccionRequest(
            @NotBlank @Size(max = 30) String alias,
            @NotBlank @Size(min = 5, max = 100) @Pattern(regexp = ".*[A-Za-zÁÉÍÓÚÑáéíóúñ]{3}.*", message = "Escribe la calle (el número es opcional)") String calle,
            @NotBlank @Size(max = 60) String comuna,
            @Size(max = 120) String referencia,
            Boolean principal) {
    }

    public record MedioPagoRequest(
            @NotBlank @Size(max = 30) String alias,
            @NotBlank @Size(max = 20) String marca,
            @NotBlank @Pattern(regexp = "^\\d{4}$", message = "Deben ser los últimos 4 dígitos") String ultimos4,
            @NotBlank @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Formato MM/AA") String vence,
            @NotBlank @Size(max = 120) String tokenPasarela) {
    }

    public record PreferenciaRequest(@Size(max = 300) String alergias, @Size(max = 500) String notas) {
    }

    public record NotaRequest(@NotNull TipoNota tipo, @NotBlank @Size(max = 500) String texto, Long autorId) {
    }

    /** Lo envía ms-pedidos cuando un pedido se entrega. */
    public record CompraRequest(@NotNull @DecimalMin("0") BigDecimal monto, Long pedidoId) {
    }
}
