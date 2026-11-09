package cl.bocadopolar.frio.controller;

import java.util.List;

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

import cl.bocadopolar.frio.dto.FrioDtos.AtenderRequest;
import cl.bocadopolar.frio.dto.FrioDtos.CuentaRequest;
import cl.bocadopolar.frio.dto.FrioDtos.RegistroResponse;
import cl.bocadopolar.frio.dto.FrioDtos.TemperaturaRequest;
import cl.bocadopolar.frio.model.AlertaFrio;
import cl.bocadopolar.frio.model.CuentaRegresivaRuta;
import cl.bocadopolar.frio.model.OrigenTemp;
import cl.bocadopolar.frio.model.RegistroTemperatura;
import cl.bocadopolar.frio.service.FrioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FrioController {

    private final FrioService service;

    /** Body: {"origen":"LOTE","referenciaId":3,"grados":-14.2,"registradoPorId":3} */
    @PostMapping("/temperaturas")
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody TemperaturaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(req.origen(), req.referenciaId(), req.grados(), req.registradoPorId()));
    }

    /** Ej: GET /api/v1/temperaturas?origen=SALIDA_RUTA  ·  ?origen=LOTE&referenciaId=3 */
    @GetMapping("/temperaturas")
    public List<RegistroTemperatura> registros(@RequestParam(name = "origen", required = false) OrigenTemp origen,
            @RequestParam(name = "referenciaId", required = false) Long referenciaId) {
        return service.registros(origen, referenciaId);
    }

    @GetMapping("/temperaturas/lote/{loteId}")
    public List<RegistroTemperatura> porLote(@PathVariable("loteId") Long loteId) {
        return service.registros(OrigenTemp.LOTE, loteId);
    }

    @GetMapping("/alertas-frio")
    public List<AlertaFrio> alertas(@RequestParam(name = "atendida", required = false) Boolean atendida) {
        return service.alertas(atendida);
    }

    @PatchMapping("/alertas-frio/{id}/atender")
    public AlertaFrio atender(@PathVariable("id") Long id, @RequestBody(required = false) AtenderRequest req) {
        return service.atender(id, req == null ? null : req.usuarioId());
    }

    @PostMapping("/cuentas-regresivas")
    public ResponseEntity<CuentaRegresivaRuta> iniciarCuenta(@Valid @RequestBody CuentaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.iniciarCuenta(req.turnoId()));
    }

    /** Devuelve minutosRestantes, debeAvisar y excedida para el temporizador del repartidor. */
    @GetMapping("/cuentas-regresivas/turno/{turnoId}")
    public CuentaRegresivaRuta cuenta(@PathVariable("turnoId") Long turnoId) {
        return service.cuenta(turnoId);
    }
}
