package cl.bocadopolar.reportes.config;

import java.time.LocalDate;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.reportes.model.VisitaDiaria;
import cl.bocadopolar.reportes.repository.VisitaDiariaRepository;
import lombok.RequiredArgsConstructor;

/** 70 días de visitas de ejemplo (45 a 94 por día) para calcular la conversión. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final VisitaDiariaRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        Random rnd = new Random(77);
        LocalDate hoy = LocalDate.now();
        for (int d = 70; d >= 1; d--) {
            repository.save(new VisitaDiaria(hoy.minusDays(d), 45 + rnd.nextInt(50)));
        }
        repository.save(new VisitaDiaria(hoy, 30));
    }
}
