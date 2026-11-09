package cl.bocadopolar.auth.dto;

import java.time.Instant;
import java.util.List;

import cl.bocadopolar.auth.model.EstadoUsuario;
import cl.bocadopolar.auth.model.Rol;
import cl.bocadopolar.auth.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cuerpos de entrada y salida de ms-auth (mismas reglas que validations.js del prototipo). */
public final class AuthDtos {

    public static final String NOMBRE_REGEX = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' -]+$";
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$";
    public static final String TELEFONO_REGEX = "^$|^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$";
    public static final String PASSWORD_MSG = "Mínimo 8 caracteres, una mayúscula, una minúscula y un número";

    private AuthDtos() {
    }

    public record RegistroRequest(
            @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = NOMBRE_REGEX, message = "Usa solo letras y espacios") String nombre,
            @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = NOMBRE_REGEX, message = "Usa solo letras y espacios") String apellido,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String password,
            @Pattern(regexp = TELEFONO_REGEX, message = "Formato: +56 9 1234 5678") String telefono,
            @Size(max = 12) String rut) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, String tipo, Instant expiraEn, Usuario usuario, List<String> permisos) {
    }

    public record ValidacionResponse(boolean valido, Long usuarioId, String email, Rol rol, String seccion,
            Boolean permitido) {
    }

    /** Alta de usuarios desde el panel (personal, repartidores o clientes). */
    public record UsuarioAdminRequest(
            @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = NOMBRE_REGEX, message = "Usa solo letras y espacios") String nombre,
            @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = NOMBRE_REGEX, message = "Usa solo letras y espacios") String apellido,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String password,
            @NotNull Rol rol,
            @Pattern(regexp = TELEFONO_REGEX, message = "Formato: +56 9 1234 5678") String telefono,
            @Size(max = 12) String rut) {
    }

    public record UsuarioUpdateRequest(
            @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = NOMBRE_REGEX, message = "Usa solo letras y espacios") String nombre,
            @NotBlank @Size(min = 2, max = 40) @Pattern(regexp = NOMBRE_REGEX, message = "Usa solo letras y espacios") String apellido,
            @Pattern(regexp = TELEFONO_REGEX, message = "Formato: +56 9 1234 5678") String telefono,
            @Size(max = 12) String rut) {
    }

    public record CambioPasswordRequest(@NotBlank String actual,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MSG) String nueva) {
    }

    public record EstadoRequest(@NotNull EstadoUsuario estado) {
    }

    public record RolRequest(@NotNull Rol rol) {
    }

    public record PermisosRequest(@NotNull List<String> secciones) {
    }

    public record PermisosRolResponse(Rol rol, List<String> secciones) {
    }
}
