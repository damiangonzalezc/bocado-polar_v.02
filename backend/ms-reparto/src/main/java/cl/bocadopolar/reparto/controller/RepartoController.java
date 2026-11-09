package cl.bocadopolar.reparto.controller;

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

import cl.bocadopolar.reparto.dto.RepartoDtos.CalificacionRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.CerrarTurnoRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.DisponibilidadRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.EntregaFallidaRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.EntregaRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.IncidenciaRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.IniciarTurnoRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.PreferenciasRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.RepartidorRequest;
import cl.bocadopolar.reparto.model.CalificacionRepartidor;
import cl.bocadopolar.reparto.model.CierreCaja;
import cl.bocadopolar.reparto.model.Disponibilidad;
import cl.bocadopolar.reparto.model.Entrega;
import cl.bocadopolar.reparto.model.Incidencia;
import cl.bocadopolar.reparto.model.Repartidor;
import cl.bocadopolar.reparto.model.Turno;
import cl.bocadopolar.reparto.service.RepartoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RepartoController {

    private final RepartoService service;

    /* ---------- Repartidores (id = id de usuario en ms-auth) ---------- */

    @GetMapping("/repartidores")
    public List<Repartidor> repartidores(@RequestParam(name = "disponibilidad", required = false) Disponibilidad d) {
        return service.listar(d);
    }

    @GetMapping("/repartidores/{id}")
    public Repartidor repartidor(@PathVariable("id") Long id) {
        return service.obtener(id);
    }

    @PostMapping("/repartidores")
    public ResponseEntity<Repartidor> registrar(@Valid @RequestBody RepartidorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    @PatchMapping("/repartidores/{id}/disponibilidad")
    public Repartidor disponibilidad(@PathVariable("id") Long id, @Valid @RequestBody DisponibilidadRequest req) {
        return service.cambiarDisponibilidad(id, req.disponibilidad());
    }

    @PatchMapping("/repartidores/{id}/preferencias")
    public Repartidor preferencias(@PathVariable("id") Long id, @Valid @RequestBody PreferenciasRequest req) {
        return service.preferencias(id, req.sonido(), req.vibracion());
    }

    /* ---------- Turnos y cierre de caja ---------- */

    @PostMapping("/turnos/iniciar")
    public ResponseEntity<Turno> iniciarTurno(@Valid @RequestBody IniciarTurnoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.iniciarTurno(req.repartidorId(), req.tempCajaInicial()));
    }

    @GetMapping("/turnos/activo/{repartidorId}")
    public Turno turnoActivo(@PathVariable("repartidorId") Long repartidorId) {
        return service.turnoActivo(repartidorId);
    }

    @PostMapping("/turnos/{id}/cerrar")
    public CierreCaja cerrarTurno(@PathVariable("id") Long id, @Valid @RequestBody CerrarTurnoRequest req) {
        return service.cerrarTurno(id, req.efectivoRendido());
    }

    @GetMapping("/cierres")
    public List<CierreCaja> cierres(@RequestParam(name = "repartidorId", required = false) Long repartidorId) {
        return service.cierres(repartidorId);
    }

    /* ---------- Entregas ---------- */

    @GetMapping("/entregas")
    public List<Entrega> entregas(@RequestParam(name = "repartidorId", required = false) Long repartidorId,
            @RequestParam(name = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.entregas(repartidorId, fecha);
    }

    @GetMapping("/entregas/{id}")
    public Entrega entrega(@PathVariable("id") Long id) {
        return service.entrega(id);
    }

    @GetMapping("/entregas/pedido/{pedidoId}")
    public List<Entrega> entregasDelPedido(@PathVariable("pedidoId") Long pedidoId) {
        return service.entregasDelPedido(pedidoId);
    }

    @PostMapping("/entregas")
    public ResponseEntity<Entrega> registrarEntrega(@Valid @RequestBody EntregaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarEntrega(req));
    }

    @PostMapping("/entregas/fallida")
    public ResponseEntity<Entrega> registrarFallida(@Valid @RequestBody EntregaFallidaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarFallida(req));
    }

    @PostMapping("/entregas/{id}/calificacion")
    public ResponseEntity<CalificacionRepartidor> calificar(@PathVariable("id") Long id,
            @Valid @RequestBody CalificacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.calificar(id, req.clienteId(), req.estrellas()));
    }

    /* ---------- Incidencias ---------- */

    @GetMapping("/incidencias")
    public List<Incidencia> incidencias(@RequestParam(name = "repartidorId", required = false) Long repartidorId) {
        return service.incidencias(repartidorId);
    }

    @PostMapping("/incidencias")
    public ResponseEntity<Incidencia> reportar(@Valid @RequestBody IncidenciaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reportarIncidencia(req));
    }
}
