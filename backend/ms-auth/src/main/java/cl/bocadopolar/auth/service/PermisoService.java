package cl.bocadopolar.auth.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.auth.dto.AuthDtos.PermisosRolResponse;
import cl.bocadopolar.auth.exception.ReglaNegocioException;
import cl.bocadopolar.auth.exception.SolicitudInvalidaException;
import cl.bocadopolar.auth.model.PermisoRol;
import cl.bocadopolar.auth.model.Rol;
import cl.bocadopolar.auth.repository.PermisoRolRepository;
import lombok.RequiredArgsConstructor;

/** Permisos por sección del panel. ADMIN siempre ve todo; CLIENTE y REPARTIDOR no usan el panel. */
@Service
@RequiredArgsConstructor
public class PermisoService {

    public static final List<String> SECCIONES = List.of("resumen", "pedidos", "inventario", "categorias",
            "clientes", "marketing", "rutas", "frio", "usuarios", "configuracion");
    public static final Set<String> SOLO_ADMIN = Set.of("usuarios", "configuracion");
    private static final Set<Rol> ROLES_PANEL = Set.of(Rol.VENDEDOR, Rol.BODEGUERO, Rol.SOPORTE);

    private final PermisoRolRepository repository;

    public boolean tienePermiso(Rol rol, String seccion) {
        if (rol == Rol.ADMIN) {
            return true;
        }
        return repository.existsByRolAndSeccionAndConcedidoTrue(rol, seccion);
    }

    public List<String> seccionesDe(Rol rol) {
        if (rol == Rol.ADMIN) {
            return SECCIONES;
        }
        return repository.findByRol(rol).stream()
                .filter(PermisoRol::getConcedido)
                .map(PermisoRol::getSeccion)
                .toList();
    }

    public List<PermisosRolResponse> listarTodos() {
        List<PermisosRolResponse> lista = new ArrayList<>();
        for (Rol rol : Rol.values()) {
            lista.add(new PermisosRolResponse(rol, seccionesDe(rol)));
        }
        return lista;
    }

    @Transactional
    public PermisosRolResponse reemplazar(Rol rol, List<String> secciones) {
        if (!ROLES_PANEL.contains(rol)) {
            throw new ReglaNegocioException("Solo se configuran permisos para VENDEDOR, BODEGUERO y SOPORTE");
        }
        for (String s : secciones) {
            if (!SECCIONES.contains(s)) {
                throw new SolicitudInvalidaException("Sección desconocida: " + s + ". Válidas: " + SECCIONES);
            }
            if (SOLO_ADMIN.contains(s)) {
                throw new ReglaNegocioException("La sección '" + s + "' es exclusiva del administrador");
            }
        }
        repository.deleteByRol(rol);
        repository.flush();
        secciones.stream().distinct().forEach(s -> repository.save(new PermisoRol(null, rol, s, true)));
        return new PermisosRolResponse(rol, seccionesDe(rol));
    }

    @Transactional
    public void sembrar(Rol rol, String... secciones) {
        Arrays.stream(secciones).forEach(s -> repository.save(new PermisoRol(null, rol, s, true)));
    }
}
