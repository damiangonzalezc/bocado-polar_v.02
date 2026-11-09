package cl.bocadopolar.configuracion.controller;

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
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.configuracion.dto.ConfigDtos.ActivaRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.EmpresaRequest;
import cl.bocadopolar.configuracion.model.EmpresaLogistica;
import cl.bocadopolar.configuracion.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/empresas-logistica")
@RequiredArgsConstructor
public class EmpresaLogisticaController {

    private final ConfigService service;

    @GetMapping
    public List<EmpresaLogistica> listar() {
        return service.listarEmpresas();
    }

    @GetMapping("/{id}")
    public EmpresaLogistica obtener(@PathVariable("id") Long id) {
        return service.obtenerEmpresa(id);
    }

    @PostMapping
    public ResponseEntity<EmpresaLogistica> crear(@Valid @RequestBody EmpresaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearEmpresa(req));
    }

    @PutMapping("/{id}")
    public EmpresaLogistica actualizar(@PathVariable("id") Long id, @Valid @RequestBody EmpresaRequest req) {
        return service.actualizarEmpresa(id, req);
    }

    @PatchMapping("/{id}/activa")
    public EmpresaLogistica activar(@PathVariable("id") Long id, @Valid @RequestBody ActivaRequest req) {
        return service.cambiarEmpresaActiva(id, req.activa());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarEmpresa(id);
        return ResponseEntity.noContent().build();
    }
}
