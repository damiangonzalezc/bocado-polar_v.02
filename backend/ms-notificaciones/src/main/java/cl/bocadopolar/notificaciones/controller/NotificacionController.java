package cl.bocadopolar.notificaciones.controller;

import java.util.List;
import java.util.Map;

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

import cl.bocadopolar.notificaciones.dto.NotificacionDtos.EventoRequest;
import cl.bocadopolar.notificaciones.dto.NotificacionDtos.PlantillaRequest;
import cl.bocadopolar.notificaciones.model.Canal;
import cl.bocadopolar.notificaciones.model.EstadoEnvio;
import cl.bocadopolar.notificaciones.model.HistorialMensaje;
import cl.bocadopolar.notificaciones.model.MotivoNotificacion;
import cl.bocadopolar.notificaciones.model.Notificacion;
import cl.bocadopolar.notificaciones.model.PlantillaMensaje;
import cl.bocadopolar.notificaciones.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService service;

    /* ---------- Notificaciones ---------- */

    /** Uso interno: los demás microservicios publican aquí sus eventos. */
    @PostMapping("/notificaciones/eventos")
    public ResponseEntity<Notificacion> evento(@Valid @RequestBody EventoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.procesarEvento(req));
    }

    @GetMapping("/notificaciones")
    public List<Notificacion> listar(@RequestParam(name = "estado", required = false) EstadoEnvio estado,
            @RequestParam(name = "motivo", required = false) MotivoNotificacion motivo,
            @RequestParam(name = "canal", required = false) Canal canal,
            @RequestParam(name = "destinatario", required = false) String destinatario) {
        return service.listar(estado, motivo, canal, destinatario);
    }

    /** Campana del panel admin: GET /api/v1/notificaciones/panel?soloNoLeidas=true */
    @GetMapping("/notificaciones/panel")
    public List<Notificacion> panel(@RequestParam(name = "soloNoLeidas", defaultValue = "false") boolean soloNoLeidas) {
        return service.panel(soloNoLeidas);
    }

    @PostMapping("/notificaciones/reintentar-fallidas")
    public Map<String, Integer> reintentar() {
        return Map.of("reintentadas", service.reintentarFallidas());
    }

    @GetMapping("/notificaciones/{id}")
    public Notificacion obtener(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @GetMapping("/notificaciones/{id}/historial")
    public List<HistorialMensaje> historial(@PathVariable("id") Long id) {
        return service.historialDe(id);
    }

    @PostMapping("/notificaciones/{id}/reenviar")
    public Notificacion reenviar(@PathVariable("id") Long id) {
        return service.reenviar(id);
    }

    @PatchMapping("/notificaciones/{id}/leida")
    public Notificacion leida(@PathVariable("id") Long id) {
        return service.marcarLeida(id);
    }

    /* ---------- Plantillas ---------- */

    @GetMapping("/plantillas")
    public List<PlantillaMensaje> plantillas() {
        return service.plantillas();
    }

    @GetMapping("/plantillas/{id}")
    public PlantillaMensaje plantilla(@PathVariable("id") Long id) {
        return service.plantilla(id);
    }

    @PostMapping("/plantillas")
    public ResponseEntity<PlantillaMensaje> crear(@Valid @RequestBody PlantillaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearPlantilla(req));
    }

    @PutMapping("/plantillas/{id}")
    public PlantillaMensaje actualizar(@PathVariable("id") Long id, @Valid @RequestBody PlantillaRequest req) {
        return service.actualizarPlantilla(id, req);
    }

    @DeleteMapping("/plantillas/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        service.eliminarPlantilla(id);
        return ResponseEntity.noContent().build();
    }
}
