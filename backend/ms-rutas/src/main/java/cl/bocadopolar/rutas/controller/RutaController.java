package cl.bocadopolar.rutas.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import cl.bocadopolar.rutas.dto.RutaDtos.AsignacionRequest;
import cl.bocadopolar.rutas.dto.RutaDtos.AsignarPedidoRequest;
import cl.bocadopolar.rutas.dto.RutaDtos.FallaRequest;
import cl.bocadopolar.rutas.dto.RutaDtos.ParadaRequest;
import cl.bocadopolar.rutas.dto.RutaDtos.RutaRequest;
import cl.bocadopolar.rutas.dto.RutaDtos.VehiculoRequest;
import cl.bocadopolar.rutas.model.EstadoRuta;
import cl.bocadopolar.rutas.model.ParadaRuta;
import cl.bocadopolar.rutas.model.Ruta;
import cl.bocadopolar.rutas.model.Vehiculo;
import cl.bocadopolar.rutas.service.RutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService service;

    /* ---------- Rutas ---------- */

    /** Ej: GET /api/v1/rutas?fecha=2026-10-17&estado=PLANIFICADA&repartidorId=5 */
    @GetMapping("/rutas")
    public List<Ruta> listar(
            @RequestParam(name = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(name = "estado", required = false) EstadoRuta estado,
            @RequestParam(name = "repartidorId", required = false) Long repartidorId) {
        return service.listar(fecha, estado, repartidorId);
    }

    @GetMapping("/rutas/{id}")
    public Ruta obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    /** CU-08.06 Mi ruta del día (repartidorId = id de usuario del repartidor). */
    @GetMapping("/rutas/repartidor/{repartidorId}/hoy")
    public Ruta rutaDelDia(@PathVariable("repartidorId") Long repartidorId) {
        return service.rutaDelDia(repartidorId);
    }

    @PostMapping("/rutas")
    public ResponseEntity<Ruta> crear(@Valid @RequestBody RutaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/rutas/{id}")
    public Ruta actualizar(@PathVariable("id") Long id, @Valid @RequestBody RutaRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/rutas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rutas/{id}/asignar")
    public Ruta asignar(@PathVariable("id") Long id, @Valid @RequestBody AsignacionRequest req) {
        return service.asignar(id, req.repartidorId(), req.repartidorNombre(), req.vehiculoId());
    }

    @PatchMapping("/rutas/{id}/iniciar")
    public Ruta iniciar(@PathVariable("id") Long id) {
        return service.iniciar(id);
    }

    @PatchMapping("/rutas/{id}/finalizar")
    public Ruta finalizar(@PathVariable("id") Long id) {
        return service.finalizar(id);
    }

    @PatchMapping("/rutas/{id}/cancelar")
    public Ruta cancelar(@PathVariable("id") Long id) {
        return service.cancelar(id);
    }

    @PatchMapping("/rutas/{id}/optimizar")
    public Ruta optimizar(@PathVariable("id") Long id) {
        return service.optimizar(id);
    }

    /* ---------- Paradas ---------- */

    /**
     * El admin asigna un pedido a un repartidor. Lo agrega como parada a la ruta del repartidor para la
     * fecha de entrega (la crea si no existe) y lo quita de la ruta donde estaba antes.
     */
    @PostMapping("/rutas/asignar-pedido")
    public Ruta asignarPedido(@Valid @RequestBody AsignarPedidoRequest req) {
        return service.asignarPedido(req.pedidoId(), req.repartidorId(), req.repartidorNombre(), req.usuarioId());
    }

    @GetMapping("/rutas/{id}/paradas")
    public List<ParadaRuta> paradas(@PathVariable("id") Long id) {
        return service.obtener(id).getParadas();
    }

    /** Trae de ms-pedidos los pedidos listos de las comunas y fecha de la ruta. */
    @PostMapping("/rutas/{id}/paradas/generar")
    public Ruta generarParadas(@PathVariable("id") Long id) {
        return service.generarParadas(id);
    }

    @PostMapping("/rutas/{id}/paradas")
    public ResponseEntity<ParadaRuta> agregarParada(@PathVariable("id") Long id, @Valid @RequestBody ParadaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarParada(id, req));
    }

    @DeleteMapping("/rutas/{id}/paradas/{paradaId}")
    public ResponseEntity<Void> quitarParada(@PathVariable("id") Long id, @PathVariable("paradaId") Long paradaId) {
        service.quitarParada(id, paradaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/paradas/{paradaId}")
    public ParadaRuta parada(@PathVariable("paradaId") Long paradaId) {
        return service.parada(paradaId);
    }

    @PatchMapping("/paradas/{paradaId}/entregada")
    public ParadaRuta entregada(@PathVariable("paradaId") Long paradaId) {
        return service.marcarEntregada(paradaId);
    }

    @PatchMapping("/paradas/{paradaId}/fallida")
    public ParadaRuta fallida(@PathVariable("paradaId") Long paradaId, @Valid @RequestBody FallaRequest req) {
        return service.marcarFallida(paradaId, req.motivo());
    }

    /* ---------- Vehículos ---------- */

    @GetMapping("/vehiculos")
    public List<Vehiculo> vehiculos() {
        return service.vehiculos();
    }

    @GetMapping("/vehiculos/{id}")
    public Vehiculo vehiculo(@PathVariable("id") Long id) {
        return service.vehiculo(id);
    }

    @PostMapping("/vehiculos")
    public ResponseEntity<Vehiculo> crearVehiculo(@Valid @RequestBody VehiculoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearVehiculo(req));
    }

    @PutMapping("/vehiculos/{id}")
    public Vehiculo actualizarVehiculo(@PathVariable("id") Long id, @Valid @RequestBody VehiculoRequest req) {
        return service.actualizarVehiculo(id, req);
    }

    @DeleteMapping("/vehiculos/{id}")
    public ResponseEntity<Void> eliminarVehiculo(@PathVariable("id") Long id) {
        service.eliminarVehiculo(id);
        return ResponseEntity.noContent().build();
    }
}
