package cl.bocadopolar.inventario.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.bocadopolar.inventario.dto.InventarioDtos.StockProducto;
import cl.bocadopolar.inventario.model.MovimientoStock;
import cl.bocadopolar.inventario.service.InventarioService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StockController {

    private final InventarioService service;

    /** Stock de todos los productos (los críticos primero). */
    @GetMapping("/stock")
    public List<StockProducto> stockGeneral() {
        return service.stockGeneral();
    }

    @GetMapping("/stock/{productoId}")
    public StockProducto stock(@PathVariable("productoId") Long productoId) {
        return service.stock(productoId);
    }

    /** Ej: GET /api/v1/movimientos?loteId=3  o  ?productoId=1  (sin filtros: últimos 200). */
    @GetMapping("/movimientos")
    public List<MovimientoStock> movimientos(@RequestParam(name = "loteId", required = false) Long loteId,
            @RequestParam(name = "productoId", required = false) Long productoId) {
        return service.movimientos(loteId, productoId);
    }
}
