package cl.bocadopolar.catalogo.controller;

import java.math.BigDecimal;
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

import cl.bocadopolar.catalogo.dto.CatalogoDtos.ActivoRequest;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.AjustePreciosRequest;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.ProductoRequest;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.ResumenValoracion;
import cl.bocadopolar.catalogo.dto.CatalogoDtos.ValoracionRequest;
import cl.bocadopolar.catalogo.model.Producto;
import cl.bocadopolar.catalogo.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final CatalogoService service;

    /** Ej: GET /api/v1/productos?q=salmon&categoriaId=1&precioMin=5000&orden=precio_asc */
    @GetMapping
    public List<Producto> buscar(@RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoriaId", required = false) Long categoriaId,
            @RequestParam(name = "precioMin", required = false) BigDecimal precioMin,
            @RequestParam(name = "precioMax", required = false) BigDecimal precioMax,
            @RequestParam(name = "incluirInactivos", defaultValue = "false") boolean incluirInactivos,
            @RequestParam(name = "orden", required = false) String orden) {
        return service.buscar(q, categoriaId, precioMin, precioMax, incluirInactivos, orden);
    }

    /** Ej: GET /api/v1/productos/por-ids?ids=1,2,3 (uso interno de ms-pedidos). */
    @GetMapping("/por-ids")
    public List<Producto> porIds(@RequestParam(name = "ids") List<Long> ids) {
        return service.porIds(ids);
    }

    @GetMapping("/sku/{sku}")
    public Producto porSku(@PathVariable("sku") String sku) {
        return service.porSku(sku);
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable("id") Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/activo")
    public Producto cambiarActivo(@PathVariable("id") Long id, @Valid @RequestBody ActivoRequest req) {
        return service.cambiarActivo(id, req.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Body: { "ids": [1,2], "porcentaje": 5 }  (ids vacío = todos) */
    @PatchMapping("/precios")
    public List<Producto> ajustarPrecios(@Valid @RequestBody AjustePreciosRequest req) {
        return service.ajustarPreciosMasivo(req.ids(), req.porcentaje());
    }

    /* ---------- Valoraciones ---------- */

    @PostMapping("/{id}/valoraciones")
    public ResumenValoracion valorar(@PathVariable("id") Long id, @Valid @RequestBody ValoracionRequest req) {
        return service.valorar(id, req.clienteId(), req.estrellas());
    }

    /** Ej: GET /api/v1/productos/1/valoraciones?clienteId=1 */
    @GetMapping("/{id}/valoraciones")
    public ResumenValoracion resumen(@PathVariable("id") Long id,
            @RequestParam(name = "clienteId", required = false) Long clienteId) {
        return service.resumen(id, clienteId);
    }
}
