package cl.bocadopolar.frio.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.frio.client.ConfigClient;
import cl.bocadopolar.frio.client.InventarioClient;
import cl.bocadopolar.frio.dto.FrioDtos.RegistroResponse;
import cl.bocadopolar.frio.exception.RecursoNoEncontradoException;
import cl.bocadopolar.frio.model.AlertaFrio;
import cl.bocadopolar.frio.model.CuentaRegresivaRuta;
import cl.bocadopolar.frio.model.OrigenTemp;
import cl.bocadopolar.frio.model.RegistroTemperatura;
import cl.bocadopolar.frio.model.Severidad;
import cl.bocadopolar.frio.repository.AlertaFrioRepository;
import cl.bocadopolar.frio.repository.CuentaRegresivaRutaRepository;
import cl.bocadopolar.frio.repository.RegistroTemperaturaRepository;
import lombok.RequiredArgsConstructor;

/**
 * RN-01: sobre -15 °C se genera ADVERTENCIA. Si un lote sigue sobre el umbral por 30 minutos o más,
 * la alerta pasa a CRITICA y el lote se bloquea en ms-inventario.
 */
@Service
@RequiredArgsConstructor
public class FrioService {

    private final RegistroTemperaturaRepository registroRepository;
    private final AlertaFrioRepository alertaRepository;
    private final CuentaRegresivaRutaRepository cuentaRepository;
    private final ConfigClient config;
    private final InventarioClient inventario;

    @Transactional
    public RegistroResponse registrar(OrigenTemp origen, Long referenciaId, Double grados, Long registradoPorId) {
        RegistroTemperatura r = registroRepository.save(
                new RegistroTemperatura(null, origen, referenciaId, grados, Instant.now(), registradoPorId));
        return new RegistroResponse(r, evaluarUmbral(r));
    }

    /** Devuelve la alerta creada (o null si la temperatura está bien o ya había una alerta igual abierta). */
    AlertaFrio evaluarUmbral(RegistroTemperatura r) {
        double umbral = config.regla("TEMP_MAX_ALERTA", BigDecimal.valueOf(-15)).doubleValue();
        if (!r.excedeUmbral(umbral)) {
            return null;
        }
        int minutos = minutosSobreUmbral(r, umbral);
        long limiteCritico = config.regla("MIN_SOSTENIDO_CRITICO", BigDecimal.valueOf(30)).longValue();
        Severidad severidad = r.getOrigen() == OrigenTemp.LOTE && minutos >= limiteCritico
                ? Severidad.CRITICA : Severidad.ADVERTENCIA;

        List<AlertaFrio> abiertas = alertaRepository.findByOrigenAndReferenciaIdAndAtendidaFalse(r.getOrigen(), r.getReferenciaId());
        boolean yaExiste = abiertas.stream().anyMatch(a -> a.getSeveridad() == severidad || a.getSeveridad() == Severidad.CRITICA);
        if (yaExiste) {
            return null;
        }
        String detalle = String.format("%s %d a %.1f °C (umbral %.1f °C, %d min sobre el umbral)",
                r.getOrigen(), r.getReferenciaId(), r.getGrados(), umbral, minutos);
        AlertaFrio alerta = alertaRepository.save(new AlertaFrio(null, r.getId(), r.getOrigen(), r.getReferenciaId(),
                severidad, detalle, minutos, false, null, Instant.now()));
        if (severidad == Severidad.CRITICA) {
            inventario.bloquearLote(r.getReferenciaId(), "Cadena de frío: " + detalle);
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("severidad", severidad.name());
        vars.put("detalle", detalle);
        inventario.alertar(vars);
        return alerta;
    }

    /** Minutos que lleva la referencia sobre el umbral, mirando las mediciones consecutivas hacia atrás. */
    int minutosSobreUmbral(RegistroTemperatura actual, double umbral) {
        Instant desde = actual.getFecha();
        for (RegistroTemperatura r : registroRepository.findByOrigenAndReferenciaIdOrderByFechaDesc(actual.getOrigen(),
                actual.getReferenciaId())) {
            if (!r.excedeUmbral(umbral)) {
                break;
            }
            desde = r.getFecha();
        }
        return (int) Duration.between(desde, actual.getFecha()).toMinutes();
    }

    public List<RegistroTemperatura> registros(OrigenTemp origen, Long referenciaId) {
        if (origen != null && referenciaId != null) {
            return registroRepository.findByOrigenAndReferenciaIdOrderByFechaDesc(origen, referenciaId);
        }
        if (origen != null) {
            return registroRepository.findByOrigenOrderByFechaDesc(origen);
        }
        return registroRepository.findTop300ByOrderByFechaDesc();
    }

    public List<AlertaFrio> alertas(Boolean atendida) {
        return atendida == null ? alertaRepository.findAllByOrderByFechaDesc()
                : alertaRepository.findByAtendidaOrderByFechaDesc(atendida);
    }

    @Transactional
    public AlertaFrio atender(Long id, Long usuarioId) {
        AlertaFrio a = alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la alerta " + id));
        a.setAtendida(true);
        a.setAtendidaPorId(usuarioId);
        return alertaRepository.save(a);
    }

    /* ---------- Cuenta regresiva de ruta ---------- */

    @Transactional
    public CuentaRegresivaRuta iniciarCuenta(Long turnoId) {
        return cuentaRepository.findByTurnoId(turnoId).orElseGet(() -> cuentaRepository.save(new CuentaRegresivaRuta(
                null, turnoId, Instant.now(),
                config.regla("RUTA_MAX_MIN", BigDecimal.valueOf(90)).intValue(),
                config.regla("AVISO_RUTA_MIN", BigDecimal.valueOf(15)).intValue())));
    }

    public CuentaRegresivaRuta cuenta(Long turnoId) {
        return cuentaRepository.findByTurnoId(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno " + turnoId + " no tiene cuenta regresiva"));
    }
}
