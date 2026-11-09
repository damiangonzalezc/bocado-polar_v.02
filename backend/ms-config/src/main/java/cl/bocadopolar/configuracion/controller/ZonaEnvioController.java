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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.configuracion.dto.ConfigDtos.ActivaRequest;
import cl.bocadopolar.configuracion.dto.ConfigDtos.TarifaResponse;
import cl.bocadopolar.configuracion.dto.ConfigDtos.ZonaRequest;
import cl.bocadopolar.configuracion.model.ZonaEnvio;
import cl.bocadopolar.configuracion.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/zonas")
@RequiredArgsConstructor
public class ZonaEnvioController {

    private final ConfigService service;

    @GetMapping
    public List<ZonaEnvio> listar(@RequestParam(name = "activas", required = false) Boolean activas) {
        return service.listarZonas(activas);
    }

    @GetMapping("/tarifa")
    public TarifaResponse tarifa(@RequestParam(name = "comuna") String comuna) {
        ZonaEnvio z = service.tarifaPara(comuna);
        return new TarifaResponse(z.getComuna(), z.getTarifa());
    }

    @GetMapping("/{id}")
    public ZonaEnvio obtener(@PathVariable("id") Long id) {
        return service.obtenerZona(id);
    }

    @PostMapping
    public ResponseEntity<ZonaEnvio> crear(@Valid @RequestBody ZonaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearZona(req));
    }

    @PutMapping("/{id}")
    public ZonaEnvio actualizar(@PathVariable("id") Long id, @Valid @RequestBody ZonaRequest req) {
        return service.actualizarZona(id, req);
    }

    @PatchMapping("/{id}/activa")
    public ZonaEnvio activar(@PathVariable("id") Long id, @Valid @RequestBody ActivaRequest req) {
        return service.cambiarZonaActiva(id, req.activa());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarZona(id);
        return ResponseEntity.noContent().build();
    }
}
