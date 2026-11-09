package cl.bocadopolar.pedidos.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

import cl.bocadopolar.pedidos.dto.PedidoDtos.AsignacionRepartidorRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CambioEstadoRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CheckoutRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CotizacionRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.CotizacionResponse;
import cl.bocadopolar.pedidos.dto.PedidoDtos.EtiquetaResponse;
import cl.bocadopolar.pedidos.dto.PedidoDtos.MotivoRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.NotaRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.PagoEventoRequest;
import cl.bocadopolar.pedidos.dto.PedidoDtos.SeguimientoResponse;
import cl.bocadopolar.pedidos.model.EstadoPedido;
import cl.bocadopolar.pedidos.model.NotaInterna;
import cl.bocadopolar.pedidos.model.Pedido;
import cl.bocadopolar.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService service;

    /** Resumen de totales antes de confirmar (no crea nada). */
    @PostMapping("/cotizar")
    public CotizacionResponse cotizar(@Valid @RequestBody CotizacionRequest req) {
        return service.cotizar(req);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Pedido> checkout(@Valid @RequestBody CheckoutRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.checkout(req));
    }

    /**
     * Ej: GET /api/v1/pedidos?estado=EN_PREPARACION&comunas=Tomé,Penco&desde=2026-10-01&hasta=2026-10-17&q=BP-1001
     */
    @GetMapping
    public List<Pedido> listar(@RequestParam(name = "estado", required = false) EstadoPedido estado,
            @RequestParam(name = "clienteId", required = false) Long clienteId,
            @RequestParam(name = "comunas", required = false) List<String> comunas,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(name = "fechaEntrega", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntrega,
            @RequestParam(name = "repartidorId", required = false) Long repartidorId,
            @RequestParam(name = "q", required = false) String q) {
        return service.listar(estado, clienteId, comunas, desde, hasta, fechaEntrega, repartidorId, q);
    }

    @GetMapping("/{id}")
    public Pedido obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @GetMapping("/codigo/{codigo}")
    public Pedido porCodigo(@PathVariable("codigo") String codigo) {
        return service.porCodigo(codigo);
    }

    /** Mis pedidos. */
    @GetMapping("/cliente/{clienteId}")
    public List<Pedido> delCliente(@PathVariable("clienteId") Long clienteId) {
        return service.delCliente(clienteId);
    }

    @GetMapping("/{id}/seguimiento")
    public SeguimientoResponse seguimiento(@PathVariable("id") Long id) {
        return service.seguimiento(id);
    }

    /** Estados a los que se puede pasar desde el actual (para la lista desplegable del panel). */
    @GetMapping("/{id}/transiciones")
    public Set<EstadoPedido> transiciones(@PathVariable("id") Long id) {
        return service.transicionesDe(id);
    }

    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable("id") Long id, @Valid @RequestBody CambioEstadoRequest req) {
        return service.cambiarEstado(id, req.estado(), req.nota(), req.usuarioId());
    }

    /** Asigna o reasigna el repartidor. Si el pedido estaba PAGADO pasa a EN_PREPARACION. */
    @PatchMapping("/{id}/repartidor")
    public Pedido asignarRepartidor(@PathVariable("id") Long id, @Valid @RequestBody AsignacionRepartidorRequest req) {
        return service.asignarRepartidor(id, req.repartidorId(), req.repartidorNombre(), req.usuarioId());
    }

    @PatchMapping("/{id}/retroceder")
    public Pedido retroceder(@PathVariable("id") Long id, @Valid @RequestBody MotivoRequest req) {
        return service.retroceder(id, req.motivo(), req.usuarioId());
    }

    @PatchMapping("/{id}/cancelar")
    public Pedido cancelar(@PathVariable("id") Long id, @Valid @RequestBody MotivoRequest req) {
        return service.cancelar(id, req.motivo(), req.usuarioId());
    }

    /** Uso interno (ms-pagos). */
    @PatchMapping("/{id}/pago")
    public Pedido registrarPago(@PathVariable("id") Long id, @Valid @RequestBody PagoEventoRequest req) {
        return service.registrarPago(id, req.evento(), req.pagoId(), req.detalle());
    }

    @GetMapping("/{id}/notas")
    public List<NotaInterna> notas(@PathVariable("id") Long id) {
        return service.notas(id);
    }

    @PostMapping("/{id}/notas")
    public ResponseEntity<NotaInterna> agregarNota(@PathVariable("id") Long id, @Valid @RequestBody NotaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarNota(id, req.texto(), req.autorId()));
    }

    @GetMapping("/{id}/etiqueta")
    public EtiquetaResponse etiqueta(@PathVariable("id") Long id) {
        return service.etiqueta(id);
    }
}
