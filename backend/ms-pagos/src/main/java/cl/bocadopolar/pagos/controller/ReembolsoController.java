package cl.bocadopolar.pagos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.pagos.dto.PagoDtos.AprobarRequest;
import cl.bocadopolar.pagos.dto.PagoDtos.RechazarRequest;
import cl.bocadopolar.pagos.model.EstadoReembolso;
import cl.bocadopolar.pagos.model.Reembolso;
import cl.bocadopolar.pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reembolsos")
@RequiredArgsConstructor
public class ReembolsoController {

    private final PagoService service;

    @GetMapping
    public List<Reembolso> listar(@RequestParam(name = "estado", required = false) EstadoReembolso estado) {
        return service.reembolsos(estado);
    }

    @PatchMapping("/{id}/aprobar")
    public Reembolso aprobar(@PathVariable("id") Long id, @RequestBody(required = false) AprobarRequest req) {
        return service.aprobarReembolso(id, req == null ? null : req.usuarioId());
    }

    @PatchMapping("/{id}/rechazar")
    public Reembolso rechazar(@PathVariable("id") Long id, @Valid @RequestBody RechazarRequest req) {
        return service.rechazarReembolso(id, req.motivo(), req.usuarioId());
    }
}
