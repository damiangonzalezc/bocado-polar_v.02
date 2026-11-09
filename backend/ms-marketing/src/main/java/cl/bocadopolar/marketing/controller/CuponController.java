package cl.bocadopolar.marketing.controller;

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
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.marketing.dto.MarketingDtos.ActivoRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.CuponRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.UsoRequest;
import cl.bocadopolar.marketing.dto.MarketingDtos.ValidacionCupon;
import cl.bocadopolar.marketing.dto.MarketingDtos.ValidarCuponRequest;
import cl.bocadopolar.marketing.model.Cupon;
import cl.bocadopolar.marketing.model.UsoCupon;
import cl.bocadopolar.marketing.service.MarketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cupones")
@RequiredArgsConstructor
public class CuponController {

    private final MarketingService service;

    @GetMapping
    public List<Cupon> listar() {
        return service.listarCupones();
    }

    @GetMapping("/{id}")
    public Cupon obtener(@PathVariable("id") Long id) {
        return service.obtenerCupon(id);
    }

    @GetMapping("/codigo/{codigo}")
    public Cupon porCodigo(@PathVariable("codigo") String codigo) {
        return service.porCodigo(codigo);
    }

    @PostMapping
    public ResponseEntity<Cupon> crear(@Valid @RequestBody CuponRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCupon(req));
    }

    @PutMapping("/{id}")
    public Cupon actualizar(@PathVariable("id") Long id, @Valid @RequestBody CuponRequest req) {
        return service.actualizarCupon(id, req);
    }

    @PatchMapping("/{id}/activo")
    public Cupon activar(@PathVariable("id") Long id, @Valid @RequestBody ActivoRequest req) {
        return service.cambiarActivo(id, req.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarCupon(id);
        return ResponseEntity.noContent().build();
    }

    /** Body: {"codigo":"BIENVENIDA10","clienteId":1,"subtotal":45000} → {"valido":true,"descuento":4500,...} */
    @PostMapping("/validar")
    public ValidacionCupon validar(@Valid @RequestBody ValidarCuponRequest req) {
        return service.validar(req.codigo(), req.clienteId(), req.subtotal());
    }

    /** Uso interno (ms-pedidos). */
    @PostMapping("/usos")
    public ResponseEntity<UsoCupon> registrarUso(@Valid @RequestBody UsoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarUso(req.codigo(), req.pedidoId(), req.clienteId()));
    }

    @GetMapping("/{id}/usos")
    public List<UsoCupon> usos(@PathVariable("id") Long id) {
        return service.usos(id);
    }
}
