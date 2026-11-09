package cl.bocadopolar.reparto.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.reparto.model.Disponibilidad;
import cl.bocadopolar.reparto.model.Entrega;
import cl.bocadopolar.reparto.model.EstadoEntrega;
import cl.bocadopolar.reparto.model.EvidenciaEntrega;
import cl.bocadopolar.reparto.model.Repartidor;
import cl.bocadopolar.reparto.model.Turno;
import cl.bocadopolar.reparto.repository.EntregaRepository;
import cl.bocadopolar.reparto.repository.RepartidorRepository;
import cl.bocadopolar.reparto.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Repartidores 5 y 6 de ms-auth; el 5 tiene turno abierto (ruta R-001 en curso). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final RepartidorRepository repartidorRepository;
    private final TurnoRepository turnoRepository;
    private final EntregaRepository entregaRepository;

    @Override
    public void run(String... args) {
        if (repartidorRepository.count() > 0) {
            return;
        }
        repartidorRepository.save(new Repartidor(5L, "Repartidor Demo", "reparto@bocado.cl", "12345678-5",
                Disponibilidad.EN_RUTA, true, true, 4.8, 31));
        repartidorRepository.save(new Repartidor(6L, "Rodrigo Norte", "reparto2@bocado.cl", "13456789-9",
                Disponibilidad.FUERA_SERVICIO, true, false, 4.6, 18));
        turnoRepository.save(new Turno(null, 5L, 1L, Instant.now().minus(1, ChronoUnit.HOURS), null, -18.0, false));
        entregaRepository.save(new Entrega(null, 6L, null, 6L, null, EstadoEntrega.ENTREGADA, "Camila Rojas", -16.0,
                null, Instant.now().minus(90, ChronoUnit.MINUTES), new EvidenciaEntrega(null, null, "firma-demo")));
        log.info("ms-reparto: datos de ejemplo creados");
    }
}
