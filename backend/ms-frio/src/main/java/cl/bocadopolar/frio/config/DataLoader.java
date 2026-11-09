package cl.bocadopolar.frio.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.frio.model.AlertaFrio;
import cl.bocadopolar.frio.model.CuentaRegresivaRuta;
import cl.bocadopolar.frio.model.OrigenTemp;
import cl.bocadopolar.frio.model.RegistroTemperatura;
import cl.bocadopolar.frio.model.Severidad;
import cl.bocadopolar.frio.repository.AlertaFrioRepository;
import cl.bocadopolar.frio.repository.CuentaRegresivaRutaRepository;
import cl.bocadopolar.frio.repository.RegistroTemperaturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Temperaturas de los lotes 1..7 de ms-inventario (el lote 7, Mix Mariscos, está a -14 °C) y el turno 1. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final RegistroTemperaturaRepository registroRepository;
    private final AlertaFrioRepository alertaRepository;
    private final CuentaRegresivaRutaRepository cuentaRepository;

    @Override
    public void run(String... args) {
        if (registroRepository.count() > 0) {
            return;
        }
        Instant ahora = Instant.now();
        double[] temps = { -18, -18, -18, -17, -16, -18, -14 };
        for (int i = 0; i < temps.length; i++) {
            registroRepository.save(new RegistroTemperatura(null, OrigenTemp.LOTE, (long) (i + 1), temps[i],
                    ahora.minus(20, ChronoUnit.MINUTES), 3L));
        }
        RegistroTemperatura mix = registroRepository.findByOrigenAndReferenciaIdOrderByFechaDesc(OrigenTemp.LOTE, 7L).get(0);
        alertaRepository.save(new AlertaFrio(null, mix.getId(), OrigenTemp.LOTE, 7L, Severidad.ADVERTENCIA,
                "LOTE 7 a -14,0 °C (umbral -15,0 °C)", 0, false, null, mix.getFecha()));
        registroRepository.saveAll(List.of(
                new RegistroTemperatura(null, OrigenTemp.SALIDA_RUTA, 1L, -18.0, ahora.minus(60, ChronoUnit.MINUTES), 5L),
                new RegistroTemperatura(null, OrigenTemp.ENTREGA, 6L, -16.0, ahora.minus(90, ChronoUnit.MINUTES), 6L)));
        cuentaRepository.save(new CuentaRegresivaRuta(null, 1L, ahora.minus(60, ChronoUnit.MINUTES), 90, 15));
        log.info("ms-frio: datos de ejemplo creados");
    }
}
