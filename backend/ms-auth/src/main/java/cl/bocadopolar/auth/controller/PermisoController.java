package cl.bocadopolar.auth.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.auth.dto.AuthDtos.PermisosRequest;
import cl.bocadopolar.auth.dto.AuthDtos.PermisosRolResponse;
import cl.bocadopolar.auth.model.Rol;
import cl.bocadopolar.auth.service.PermisoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/permisos")
@RequiredArgsConstructor
public class PermisoController {

    private final PermisoService service;

    @GetMapping
    public List<PermisosRolResponse> listar() {
        return service.listarTodos();
    }

    @GetMapping("/secciones")
    public List<String> secciones() {
        return PermisoService.SECCIONES;
    }

    @GetMapping("/rol/{rol}")
    public PermisosRolResponse porRol(@PathVariable("rol") Rol rol) {
        return new PermisosRolResponse(rol, service.seccionesDe(rol));
    }

    /** Reemplaza las secciones del rol. Body: { "secciones": ["resumen","pedidos"] } */
    @PutMapping("/rol/{rol}")
    public PermisosRolResponse reemplazar(@PathVariable("rol") Rol rol, @Valid @RequestBody PermisosRequest req) {
        return service.reemplazar(rol, req.secciones());
    }
}
