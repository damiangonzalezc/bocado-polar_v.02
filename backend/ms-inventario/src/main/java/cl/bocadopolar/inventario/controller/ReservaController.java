package cl.bocadopolar.inventario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.inventario.dto.InventarioDtos.ReservaRequest;
import cl.bocadopolar.inventario.model.EstadoReserva;
import cl.bocadopolar.inventario.model.ReservaStock;
import cl.bocadopolar.inventario.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Uso interno de ms-pedidos. */
@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final InventarioService service;

    @PostMapping
    public ResponseEntity<List<ReservaStock>> reservar(@Valid @RequestBody ReservaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reservar(req));
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<ReservaStock> delPedido(@PathVariable("pedidoId") Long pedidoId) {
        return service.reservasDe(pedidoId);
    }

    @PatchMapping("/pedido/{pedidoId}/confirmar")
    public List<ReservaStock> confirmar(@PathVariable("pedidoId") Long pedidoId) {
        return service.confirmar(pedidoId);
    }

    @PatchMapping("/pedido/{pedidoId}/liberar")
    public List<ReservaStock> liberar(@PathVariable("pedidoId") Long pedidoId) {
        return service.liberar(pedidoId, EstadoReserva.LIBERADA);
    }
}
