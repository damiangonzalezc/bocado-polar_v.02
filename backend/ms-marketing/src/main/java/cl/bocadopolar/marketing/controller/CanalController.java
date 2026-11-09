package cl.bocadopolar.marketing.controller;

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

import cl.bocadopolar.marketing.dto.MarketingDtos.CanalRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.ConversionCanalRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.VisitaCanalRequest;
import cl.bocadopolar.marketing.model.CanalCampana;
import cl.bocadopolar.marketing.service.MarketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/canales")
@RequiredArgsConstructor
public class CanalController {

    private final MarketingService service;

    @GetMapping
    public List<CanalCampana> listar() {
        return service.canales();
    }

    @PostMapping
    public ResponseEntity<CanalCampana> crear(@Valid @RequestBody CanalRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCanal(req));
    }

    @PutMapping("/{id}")
    public CanalCampana actualizar(@PathVariable("id") Long id, @Valid @RequestBody CanalRequest req) {
        return service.actualizarCanal(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarCanal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/visita")
    public CanalCampana visita(@Valid @RequestBody VisitaCanalRequest req) {
        return service.registrarVisita(req.canal());
    }

    @PostMapping("/conversion")
    public CanalCampana conversion(@Valid @RequestBody ConversionCanalRequest req) {
        return service.registrarConversion(req.canal(), req.monto());
    }
}
