package cl.bocadopolar.pedidos.controller;

import java.util.List;

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

import cl.bocadopolar.pedidos.dto.PedidoDtos.AsociarClienteRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CantidadRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CarritoRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.ItemCarritoRequest;
import cl.bocadopolar.pedidos.model.Carrito;
import cl.bocadopolar.pedidos.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/carritos")
@RequiredArgsConstructor
public class CarritoController {

    private final CarritoService service;

    /** Obtiene o crea el carrito. Body: {"clienteId":1} o {"sesionAnonimaId":"uuid"} o {} (nuevo visitante). */
    @PostMapping
    public Carrito obtenerOCrear(@RequestBody(required = false) CarritoRequest req) {
        return service.obtenerOCrear(req);
    }

    /** Uso interno (ms-marketing): carritos con productos sin cambios hace N horas. */
    @GetMapping("/inactivos")
    public List<Carrito> inactivos(@RequestParam(name = "horas", defaultValue = "2") int horas) {
        return service.inactivos(horas);
    }

    @GetMapping("/{id}")
    public Carrito obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @GetMapping("/cliente/{clienteId}")
    public Carrito delCliente(@PathVariable("clienteId") Long clienteId) {
        return service.delCliente(clienteId);
    }

    @PostMapping("/{id}/items")
    public Carrito agregar(@PathVariable("id") Long id, @Valid @RequestBody ItemCarritoRequest req) {
        return service.agregar(id, req.productoId(), req.kg());
    }

    @PutMapping("/{id}/items/{productoId}")
    public Carrito fijar(@PathVariable("id") Long id, @PathVariable("productoId") Long productoId,
            @Valid @RequestBody CantidadRequest req) {
        return service.fijar(id, productoId, req.kg());
    }

    @DeleteMapping("/{id}/items/{productoId}")
    public Carrito quitar(@PathVariable("id") Long id, @PathVariable("productoId") Long productoId) {
        return service.quitar(id, productoId);
    }

    @DeleteMapping("/{id}/items")
    public Carrito vaciar(@PathVariable("id") Long id) {
        return service.vaciar(id);
    }

    /** Al iniciar sesión: une el carrito del visitante con el del cliente. */
    @PatchMapping("/{id}/cliente")
    public Carrito asociarCliente(@PathVariable("id") Long id, @Valid @RequestBody AsociarClienteRequest req) {
        return service.asociarCliente(id, req.clienteId());
    }

    @PostMapping("/{id}/volver-a-pedir/{pedidoId}")
    public Carrito volverAPedir(@PathVariable("id") Long id, @PathVariable("pedidoId") Long pedidoId) {
        return service.volverAPedir(id, pedidoId);
    }
}
