package cl.bocadopolar.auth.service;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.auth.client.ClientesClient;
import cl.bocadopolar.auth.dto.AuthDtos.AuthResponse;
import cl.bocadopolar.auth.dto.AuthDtos.CambioPasswordRequest;
import cl.bocadopolar.auth.dto.AuthDtos.LoginRequest;
import cl.bocadopolar.auth.dto.AuthDtos.RegistroRequest;
import cl.bocadopolar.auth.dto.AuthDtos.ValidacionResponse;
import cl.bocadopolar.auth.exception.AccesoDenegadoException;
import cl.bocadopolar.auth.exception.NoAutorizadoException;
import cl.bocadopolar.auth.exception.ReglaNegocioException;
import cl.bocadopolar.auth.model.EstadoUsuario;
import cl.bocadopolar.auth.model.Rol;
import cl.bocadopolar.auth.model.SesionToken;
import cl.bocadopolar.auth.model.Usuario;
import cl.bocadopolar.auth.repository.SesionTokenRepository;
import cl.bocadopolar.auth.repository.UsuarioRepository;
import cl.bocadopolar.auth.security.JwtService;
import cl.bocadopolar.auth.security.JwtService.TokenGenerado;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SesionTokenRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PermisoService permisoService;
    private final AuditoriaService auditoria;
    private final ClientesClient clientesClient;

    /** CU-01.01 Registrar cuenta de cliente. Devuelve la sesión iniciada. */
    @Transactional
    public AuthResponse registrar(RegistroRequest req, String ip) {
        String email = Validaciones.correo(req.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con el correo " + email);
        }
        Usuario u = new Usuario(null, req.nombre().trim(), req.apellido().trim(), email,
                passwordEncoder.encode(req.password()), Validaciones.rut(req.rut()), limpiar(req.telefono()),
                Rol.CLIENTE, EstadoUsuario.ACTIVO, false, Instant.now());
        u = usuarioRepository.save(u);
        auditoria.registrar(u.getId(), email, "REGISTRO", true, ip);
        clientesClient.crearPerfil(u);
        return emitirSesion(u);
    }

    /** CU-01.02 Iniciar sesión. Solo las cuentas ACTIVAS pueden entrar. */
    @Transactional
    public AuthResponse login(LoginRequest req, String ip) {
        String email = req.email().trim().toLowerCase();
        Usuario u = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
        if (u == null || u.getEstado() == EstadoUsuario.ELIMINADO
                || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            auditoria.registrar(u == null ? null : u.getId(), email, "LOGIN", false, ip);
            throw new NoAutorizadoException("Correo o contraseña incorrectos");
        }
        if (u.getEstado() == EstadoUsuario.BLOQUEADO) {
            auditoria.registrar(u.getId(), email, "LOGIN_BLOQUEADO", false, ip);
            throw new AccesoDenegadoException("Tu cuenta está bloqueada. Contacta a soporte.");
        }
        if (u.getEstado() == EstadoUsuario.SUSPENDIDO) {
            auditoria.registrar(u.getId(), email, "LOGIN_SUSPENDIDO", false, ip);
            throw new AccesoDenegadoException("Tu cuenta está suspendida temporalmente.");
        }
        auditoria.registrar(u.getId(), email, "LOGIN", true, ip);
        return emitirSesion(u);
    }

    /** CU-01.03 Cerrar sesión: revoca el token actual. */
    @Transactional
    public void logout(String token, String ip) {
        Claims claims = jwtService.leer(token);
        sesionRepository.findByJti(claims.getId()).ifPresent(s -> {
            s.revocar();
            sesionRepository.save(s);
        });
        auditoria.registrar(JwtService.usuarioId(claims), claims.getSubject(), "LOGOUT", true, ip);
    }

    /** Devuelve el usuario dueño del token si la sesión sigue activa. */
    public Usuario usuarioDelToken(String token) {
        Claims claims = jwtService.leer(token);
        SesionToken sesion = sesionRepository.findByJti(claims.getId())
                .orElseThrow(() -> new NoAutorizadoException("Sesión desconocida"));
        if (!sesion.activa()) {
            throw new NoAutorizadoException("La sesión expiró o fue cerrada");
        }
        Usuario u = usuarioRepository.findById(sesion.getUsuarioId())
                .orElseThrow(() -> new NoAutorizadoException("El usuario ya no existe"));
        if (u.getEstado() != EstadoUsuario.ACTIVO) {
            throw new AccesoDenegadoException("La cuenta no está activa");
        }
        return u;
    }

    /** CU-01.07 Validar token y permiso de sección (lo puede usar el gateway o el front). */
    public ValidacionResponse validar(String token, String seccion) {
        Usuario u = usuarioDelToken(token);
        Boolean permitido = seccion == null ? null : permisoService.tienePermiso(u.getRol(), seccion);
        return new ValidacionResponse(true, u.getId(), u.getEmail(), u.getRol(), seccion, permitido);
    }

    @Transactional
    public void cambiarPassword(String token, CambioPasswordRequest req, String ip) {
        Usuario u = usuarioDelToken(token);
        if (!passwordEncoder.matches(req.actual(), u.getPasswordHash())) {
            auditoria.registrar(u.getId(), u.getEmail(), "CAMBIO_PASSWORD", false, ip);
            throw new ReglaNegocioException("La contraseña actual no es correcta");
        }
        u.setPasswordHash(passwordEncoder.encode(req.nueva()));
        usuarioRepository.save(u);
        auditoria.registrar(u.getId(), u.getEmail(), "CAMBIO_PASSWORD", true, ip);
    }

    private AuthResponse emitirSesion(Usuario u) {
        TokenGenerado t = jwtService.generar(u);
        sesionRepository.save(new SesionToken(null, u.getId(), t.jti(), t.emitido(), t.expira(), false));
        return new AuthResponse(t.token(), "Bearer", t.expira(), u, permisoService.seccionesDe(u.getRol()));
    }

    private static String limpiar(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
