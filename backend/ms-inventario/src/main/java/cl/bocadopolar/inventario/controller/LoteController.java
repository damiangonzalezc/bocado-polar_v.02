package cl.bocadopolar.inventario.controller;

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

import cl.bocadopolar.inventario.dto.InventarioDtos.AjusteMasivoRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.AjusteRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.BloqueoRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.LoteRequest;
import cl.bocadopolar.inventario.dto.InventarioDtos.LoteUpdateRequest;
import cl.bocadopolar.inventario.model.Lote;
import cl.bocadopolar.inventario.model.MovimientoStock;
import cl.bocadopolar.inventario.service.InventarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/lotes")
@RequiredArgsConstructor
public class LoteController {

    private final InventarioService service;

    /** Ej: GET /api/v1/lotes?productoId=1&bloqueado=false&incluirAgotados=true */
    @GetMapping
    public List<Lote> listar(@RequestParam(name = "productoId", required = false) Long productoId,
            @RequestParam(name = "bloqueado", required = false) Boolean bloqueado,
            @RequestParam(name = "incluirAgotados", defaultValue = "false") boolean incluirAgotados) {
        return service.listarLotes(productoId, bloqueado, incluirAgotados);
    }

    @GetMapping("/{id}")
    public Lote obtener(@PathVariable("id") Long id) {
        return service.obtenerLote(id);
    }

    @PostMapping
    public ResponseEntity<Lote> registrar(@Valid @RequestBody LoteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarLote(req));
    }

    @PutMapping("/{id}")
    public Lote actualizar(@PathVariable("id") Long id, @Valid @RequestBody LoteUpdateRequest req) {
        return service.actualizarLote(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarLote(id);
        return ResponseEntity.noContent().build();
    }

    /** Lotes utilizables del producto, primero el que vence antes. */
    @GetMapping("/fefo/{productoId}")
    public List<Lote> fefo(@PathVariable("productoId") Long productoId) {
        return service.fefo(productoId);
    }

    @PostMapping("/{id}/ajustes")
    public ResponseEntity<MovimientoStock> ajustar(@PathVariable("id") Long id, @Valid @RequestBody AjusteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajustar(id, req));
    }

    @PostMapping("/ajustes/masivo")
    public ResponseEntity<List<MovimientoStock>> ajustarMasivo(@Valid @RequestBody AjusteMasivoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajustarMasivo(req.ajustes(), req.usuarioId()));
    }

    /** Lo usa ms-frio (alerta crítica) o el bodeguero. */
    @PatchMapping("/{id}/bloqueo")
    public Lote bloquear(@PathVariable("id") Long id, @Valid @RequestBody BloqueoRequest req) {
        return service.bloquear(id, req.motivo());
    }

    @PatchMapping("/{id}/desbloqueo")
    public Lote desbloquear(@PathVariable("id") Long id) {
        return service.desbloquear(id);
    }
}
