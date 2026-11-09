package cl.bocadopolar.auth.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.auth.dto.AuthDtos.EstadoRequest;
import cl.bocadopolar.auth.dto.AuthDtos.RolRequest;
import cl.bocadopolar.auth.dto.AuthDtos.UsuarioAdminRequest;
import cl.bocadopolar.auth.dto.AuthDtos.UsuarioUpdateRequest;
import cl.bocadopolar.auth.model.EstadoUsuario;
import cl.bocadopolar.auth.model.Rol;
import cl.bocadopolar.auth.model.Usuario;
import cl.bocadopolar.auth.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    /** Ej: GET /api/v1/usuarios?rol=REPARTIDOR&estado=ACTIVO&q=juan */
    @GetMapping
    public List<Usuario> listar(@RequestParam(name = "rol", required = false) Rol rol,
            @RequestParam(name = "estado", required = false) EstadoUsuario estado,
            @RequestParam(name = "q", required = false) String q) {
        return service.listar(rol, estado, q);
    }

    @GetMapping("/{id}")
    public Usuario obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@Valid @RequestBody UsuarioAdminRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public Usuario actualizar(@PathVariable("id") Long id, @Valid @RequestBody UsuarioUpdateRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/estado")
    public Usuario cambiarEstado(@PathVariable("id") Long id, @Valid @RequestBody EstadoRequest req) {
        return service.cambiarEstado(id, req.estado());
    }

    @PatchMapping("/{id}/rol")
    public Usuario cambiarRol(@PathVariable("id") Long id, @Valid @RequestBody RolRequest req) {
        return service.cambiarRol(id, req.rol());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
