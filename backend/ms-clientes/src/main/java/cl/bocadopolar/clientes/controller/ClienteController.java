package cl.bocadopolar.clientes.controller;

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

import cl.bocadopolar.clientes.dto.ClienteDtos.ClienteRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.CompraRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.DireccionRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.MedioPagoRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.NotaRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.PerfilRequest;
import cl.bocadopolar.clientes.dto.ClienteDtos.PreferenciaRequest;
import cl.bocadopolar.clientes.model.Cliente;
import cl.bocadopolar.clientes.model.DireccionEntrega;
import cl.bocadopolar.clientes.model.MedioPago;
import cl.bocadopolar.clientes.model.NotaCliente;
import cl.bocadopolar.clientes.model.PreferenciaCliente;
import cl.bocadopolar.clientes.model.SegmentoCliente;
import cl.bocadopolar.clientes.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    /* ---------- Ficha / directorio CRM ---------- */

    /** Ej: GET /api/v1/clientes?segmento=VIP&q=soto */
    @GetMapping
    public List<Cliente> listar(@RequestParam(name = "segmento", required = false) SegmentoCliente segmento,
            @RequestParam(name = "q", required = false) String q) {
        return service.listar(segmento, q);
    }

    @GetMapping("/{id}")
    public Cliente obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @GetMapping("/usuario/{usuarioId}")
    public Cliente porUsuario(@PathVariable("usuarioId") Long usuarioId) {
        return service.porUsuario(usuarioId);
    }

    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody ClienteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    /** Uso interno (ms-auth): crea la ficha al registrarse o devuelve la existente. */
    @PostMapping("/desde-usuario")
    public Cliente desdeUsuario(@Valid @RequestBody ClienteRequest req) {
        return service.obtenerOCrearDesdeUsuario(req);
    }

    @PutMapping("/{id}")
    public Cliente actualizarPerfil(@PathVariable("id") Long id, @Valid @RequestBody PerfilRequest req) {
        return service.actualizarPerfil(id, req);
    }

    /* ---------- Direcciones ---------- */

    @GetMapping("/{id}/direcciones")
    public List<DireccionEntrega> direcciones(@PathVariable("id") Long id) {
        return service.direcciones(id);
    }

    @PostMapping("/{id}/direcciones")
    public ResponseEntity<DireccionEntrega> agregarDireccion(@PathVariable("id") Long id,
            @Valid @RequestBody DireccionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarDireccion(id, req));
    }

    @PutMapping("/{id}/direcciones/{direccionId}")
    public DireccionEntrega actualizarDireccion(@PathVariable("id") Long id,
            @PathVariable("direccionId") Long direccionId, @Valid @RequestBody DireccionRequest req) {
        return service.actualizarDireccion(id, direccionId, req);
    }

    @PatchMapping("/{id}/direcciones/{direccionId}/principal")
    public DireccionEntrega marcarPrincipal(@PathVariable("id") Long id,
            @PathVariable("direccionId") Long direccionId) {
        return service.marcarPrincipal(id, direccionId);
    }

    @DeleteMapping("/{id}/direcciones/{direccionId}")
    public ResponseEntity<Void> eliminarDireccion(@PathVariable("id") Long id,
            @PathVariable("direccionId") Long direccionId) {
        service.eliminarDireccion(id, direccionId);
        return ResponseEntity.noContent().build();
    }

    /* ---------- Medios de pago ---------- */

    @GetMapping("/{id}/medios-pago")
    public List<MedioPago> mediosPago(@PathVariable("id") Long id) {
        return service.mediosPago(id);
    }

    @PostMapping("/{id}/medios-pago")
    public ResponseEntity<MedioPago> agregarMedioPago(@PathVariable("id") Long id,
            @Valid @RequestBody MedioPagoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarMedioPago(id, req));
    }

    @DeleteMapping("/{id}/medios-pago/{medioId}")
    public ResponseEntity<Void> eliminarMedioPago(@PathVariable("id") Long id, @PathVariable("medioId") Long medioId) {
        service.eliminarMedioPago(id, medioId);
        return ResponseEntity.noContent().build();
    }

    /* ---------- Preferencias ---------- */

    @GetMapping("/{id}/preferencias")
    public PreferenciaCliente preferencias(@PathVariable("id") Long id) {
        return service.preferencias(id);
    }

    @PutMapping("/{id}/preferencias")
    public PreferenciaCliente actualizarPreferencias(@PathVariable("id") Long id,
            @Valid @RequestBody PreferenciaRequest req) {
        return service.actualizarPreferencias(id, req);
    }

    /* ---------- Notas CRM ---------- */

    @GetMapping("/{id}/notas")
    public List<NotaCliente> notas(@PathVariable("id") Long id) {
        return service.notas(id);
    }

    @PostMapping("/{id}/notas")
    public ResponseEntity<NotaCliente> agregarNota(@PathVariable("id") Long id, @Valid @RequestBody NotaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarNota(id, req));
    }

    /* ---------- Compras y segmentación ---------- */

    /** Uso interno (ms-pedidos): suma la compra entregada y recalcula el segmento. */
    @PostMapping("/{id}/compras")
    public Cliente registrarCompra(@PathVariable("id") Long id, @Valid @RequestBody CompraRequest req) {
        return service.registrarCompra(id, req.monto());
    }

    @PostMapping("/segmentar")
    public List<Cliente> segmentar() {
        return service.recalcularSegmentos();
    }
}
