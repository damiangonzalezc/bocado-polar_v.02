package cl.bocadopolar.pagos.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import cl.bocadopolar.pagos.dto.PagoDtos.CobroEntregaRequest;
import cl.bocadopolar.pagos.dto.PagoDtos.IniciarPagoRequest;
import cl.bocadopolar.pagos.dto.PagoDtos.ReembolsoRequest;
import cl.bocadopolar.pagos.dto.PagoDtos.WebhookRequest;
import cl.bocadopolar.pagos.model.CobroContraEntrega;
import cl.bocadopolar.pagos.model.EstadoPago;
import cl.bocadopolar.pagos.model.MetodoPago;
import cl.bocadopolar.pagos.model.Pago;
import cl.bocadopolar.pagos.model.Reembolso;
import cl.bocadopolar.pagos.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService service;

    @PostMapping
    public ResponseEntity<Pago> iniciar(@Valid @RequestBody IniciarPagoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.iniciar(req));
    }

    @GetMapping
    public List<Pago> listar(@RequestParam(name = "estado", required = false) EstadoPago estado,
            @RequestParam(name = "metodo", required = false) MetodoPago metodo) {
        return service.listar(estado, metodo);
    }

    /** Ej: GET /api/v1/pagos/cobros?repartidorId=5&fecha=2026-10-17 (cierre de caja). */
    @GetMapping("/cobros")
    public List<CobroContraEntrega> cobros(@RequestParam(name = "repartidorId") Long repartidorId,
            @RequestParam(name = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.cobrosDe(repartidorId, fecha);
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<Pago> delPedido(@PathVariable("pedidoId") Long pedidoId) {
        return service.delPedido(pedidoId);
    }

    @GetMapping("/{id}")
    public Pago obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    /** Simula la respuesta de Webpay: {"pagoId":1,"aprobado":true,"codigoAutorizacion":"123456"} */
    @PostMapping("/webhook")
    public Pago webhook(@Valid @RequestBody WebhookRequest req) {
        return service.webhook(req);
    }

    @PatchMapping("/{id}/confirmar-transferencia")
    public Pago confirmarTransferencia(@PathVariable("id") Long id) {
        return service.confirmarTransferencia(id);
    }

    @GetMapping("/{id}/cobro")
    public CobroContraEntrega cobro(@PathVariable("id") Long id) {
        return service.cobroDe(id);
    }

    @PatchMapping("/{id}/cobro-entrega")
    public CobroContraEntrega cobrar(@PathVariable("id") Long id, @Valid @RequestBody CobroEntregaRequest req) {
        return service.confirmarCobroEntrega(id, req);
    }

    @PostMapping("/{id}/reembolsos")
    public ResponseEntity<Reembolso> solicitarReembolso(@PathVariable("id") Long id,
            @Valid @RequestBody ReembolsoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitarReembolso(id, req));
    }
}
