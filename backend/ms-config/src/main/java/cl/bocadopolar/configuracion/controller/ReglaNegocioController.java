package cl.bocadopolar.configuracion.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.configuracion.dto.ConfigDtos.ReglaRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.ReglaValorRequest;
import cl.bocadopolar.configuracion.model.ReglaNegocio;
import cl.bocadopolar.configuracion.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reglas")
@RequiredArgsConstructor
public class ReglaNegocioController {

    private final ConfigService service;

    @GetMapping
    public List<ReglaNegocio> listar() {
        return service.listarReglas();
    }

    @GetMapping("/{codigo}")
    public ReglaNegocio obtener(@PathVariable("codigo") String codigo) {
        return service.obtenerRegla(codigo);
    }

    @PostMapping
    public ResponseEntity<ReglaNegocio> crear(@Valid @RequestBody ReglaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearRegla(req));
    }

    @PutMapping("/{codigo}")
    public ReglaNegocio actualizar(@PathVariable("codigo") String codigo, @Valid @RequestBody ReglaValorRequest req) {
        return service.actualizarRegla(codigo, req);
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> eliminar(@PathVariable("codigo") String codigo) {
        service.eliminarRegla(codigo);
        return ResponseEntity.noContent().build();
    }
}
