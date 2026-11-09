package cl.bocadopolar.inventario.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.inventario.model.AlertaStock;
import cl.bocadopolar.inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/alertas-stock")
@RequiredArgsConstructor
public class AlertaStockController {

    private final InventarioService service;

    /** Ej: GET /api/v1/alertas-stock?atendida=false */
    @GetMapping
    public List<AlertaStock> listar(@RequestParam(name = "atendida", required = false) Boolean atendida) {
        return service.alertas(atendida);
    }

    @PostMapping("/detectar")
    public List<AlertaStock> detectar() {
        return service.detectarStockCritico();
    }

    @PatchMapping("/{id}/resolver")
    public AlertaStock resolver(@PathVariable("id") Long id) {
        return service.resolverAlerta(id);
    }
}
