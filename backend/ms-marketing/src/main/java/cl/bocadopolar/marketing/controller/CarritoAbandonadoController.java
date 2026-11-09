package cl.bocadopolar.marketing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.marketing.dto.MarketingDtos.AbandonadoRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.RecuperadoRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.RescateRequest;
import cl.bocadopolar.marketing.model.CarritoAbandonado;
import cl.bocadopolar.marketing.service.MarketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/carritos-abandonados")
@RequiredArgsConstructor
public class CarritoAbandonadoController {

    private final MarketingService service;

    @GetMapping
    public List<CarritoAbandonado> listar(@RequestParam(name = "recuperado", required = false) Boolean recuperado) {
        return service.abandonados(recuperado);
    }

    @PostMapping
    public ResponseEntity<CarritoAbandonado> registrar(@Valid @RequestBody AbandonadoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarAbandonado(req));
    }

    /** Busca en ms-pedidos carritos sin movimiento hace N horas. */
    @PostMapping("/detectar")
    public List<CarritoAbandonado> detectar(@RequestParam(name = "horas", defaultValue = "2") int horas) {
        return service.detectar(horas);
    }

    /** Body opcional: {"codigoCupon":"POLAR5000"} */
    @PostMapping("/{id}/rescate")
    public CarritoAbandonado rescate(@PathVariable("id") Long id, @Valid @RequestBody(required = false) RescateRequest req) {
        return service.enviarRescate(id, req == null ? null : req.codigoCupon());
    }

    @PatchMapping("/{id}/recuperado")
    public CarritoAbandonado recuperado(@PathVariable("id") Long id, @RequestBody(required = false) RecuperadoRequest req) {
        return service.marcarRecuperado(id, req == null ? null : req.pedidoId());
    }
}
