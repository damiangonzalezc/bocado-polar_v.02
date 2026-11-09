package cl.bocadopolar.pagos.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.pagos.model.Boleta;
import cl.bocadopolar.pagos.service.PagoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/boletas")
@RequiredArgsConstructor
public class BoletaController {

    private final PagoService service;

    @GetMapping("/{id}")
    public Boleta obtener(@PathVariable("id") Long id) {
        return service.boleta(id);
    }

    @GetMapping("/pedido/{pedidoId}")
    public Boleta delPedido(@PathVariable("pedidoId") Long pedidoId) {
        return service.boletaDePedido(pedidoId);
    }

    /** Boleta imprimible: el front puede abrirla en una pestaña nueva (window.open). */
    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    public String html(@PathVariable("id") Long id) {
        return service.boletaHtml(id);
    }
}
