package cl.bocadopolar.auth.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.auth.client.ClientesClient;
import cl.bocadopolar.auth.client.RepartoClient;
import cl.bocadopolar.auth.dto.AuthDtos.UsuarioAdminRequest;
import cl.bocadopolar.auth.dto.AuthDtos.UsuarioUpdateRequest;
import cl.bocadopolar.auth.exception.RecursoNoEncontradoException;
import cl.bocadopolar.auth.exception.ReglaNegocioException;
import cl.bocadopolar.auth.model.EstadoUsuario;
import cl.bocadopolar.auth.model.Rol;
import cl.bocadopolar.auth.model.Usuario;
import cl.bocadopolar.auth.repository.SesionTokenRepository;
import cl.bocadopolar.auth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** Gestión de usuarios desde el panel (admin-usuarios.html). */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repository;
    private final SesionTokenRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientesClient clientesClient;
    private final RepartoClient repartoClient;

    public List<Usuario> listar(Rol rol, EstadoUsuario estado, String q) {
        String filtro = q == null ? "" : q.trim().toLowerCase();
        return repository.findAll().stream()
                .filter(u -> rol == null || u.getRol() == rol)
                .filter(u -> estado == null || u.getEstado() == estado)
                .filter(u -> filtro.isEmpty() || u.getEmail().toLowerCase().contains(filtro)
                        || u.nombreCompleto().toLowerCase().contains(filtro))
                .sorted(Comparator.comparing(Usuario::getId))
                .toList();
    }

    public Usuario obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }

    @Transactional
    public Usuario crear(UsuarioAdminRequest req) {
        String email = Validaciones.correo(req.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe una cuenta con el correo " + email);
        }
        if (req.rol() == Rol.ADMIN) {
            throw new ReglaNegocioException("No se pueden crear administradores desde el panel");
        }
        Usuario u = new Usuario(null, req.nombre().trim(), req.apellido().trim(), email,
                passwordEncoder.encode(req.password()), Validaciones.rut(req.rut()),
                req.telefono() == null || req.telefono().isBlank() ? null : req.telefono().trim(),
                req.rol(), EstadoUsuario.ACTIVO, false, Instant.now());
        u = repository.save(u);
        if (u.getRol() == Rol.CLIENTE) {
            clientesClient.crearPerfil(u);
        } else if (u.getRol() == Rol.REPARTIDOR) {
            repartoClient.crearRepartidor(u);
        }
        return u;
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioUpdateRequest req) {
        Usuario u = obtener(id);
        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setTelefono(req.telefono() == null || req.telefono().isBlank() ? null : req.telefono().trim());
        u.setRut(Validaciones.rut(req.rut()));
        return repository.save(u);
    }

    /** CU-01.04 Activo / Bloqueado / Eliminado / Suspendido. El admin original no se toca. */
    @Transactional
    public Usuario cambiarEstado(Long id, EstadoUsuario nuevo) {
        Usuario u = obtener(id);
        exigirNoProtegido(u, "cambiar el estado");
        u.setEstado(nuevo);
        if (nuevo != EstadoUsuario.ACTIVO) {
            revocarSesiones(u.getId());
        }
        return repository.save(u);
    }

    /** CU-01.05 Cambiar rol. */
    @Transactional
    public Usuario cambiarRol(Long id, Rol rol) {
        Usuario u = obtener(id);
        exigirNoProtegido(u, "cambiar el rol");
        if (rol == Rol.ADMIN) {
            throw new ReglaNegocioException("No se puede asignar el rol ADMIN desde el panel");
        }
        Rol anterior = u.getRol();
        u.setRol(rol);
        revocarSesiones(u.getId());
        Usuario guardado = repository.save(u);
        if (rol == Rol.REPARTIDOR && anterior != Rol.REPARTIDOR) {
            repartoClient.crearRepartidor(guardado);
        } else if (rol == Rol.CLIENTE && anterior != Rol.CLIENTE) {
            clientesClient.crearPerfil(guardado);
        }
        return guardado;
    }

    /** Eliminación lógica: el usuario queda en estado ELIMINADO (se conserva el historial). */
    @Transactional
    public void eliminar(Long id) {
        cambiarEstado(id, EstadoUsuario.ELIMINADO);
    }

    private void exigirNoProtegido(Usuario u, String accion) {
        if (Boolean.TRUE.equals(u.getProtegido())) {
            throw new ReglaNegocioException("No se puede " + accion + " del administrador original");
        }
    }

    private void revocarSesiones(Long usuarioId) {
        sesionRepository.findByUsuarioIdAndRevocadoFalse(usuarioId).forEach(s -> {
            s.revocar();
            sesionRepository.save(s);
        });
    }
}
