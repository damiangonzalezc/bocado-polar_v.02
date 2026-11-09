package cl.bocadopolar.marketing.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.marketing.model.CanalCampana;
import cl.bocadopolar.marketing.model.CarritoAbandonado;
import cl.bocadopolar.marketing.model.Cupon;
import cl.bocadopolar.marketing.model.TipoDescuento;
import cl.bocadopolar.marketing.repository.CanalCampanaRepository;
import cl.bocadopolar.marketing.repository.CarritoAbandonadoRepository;
import cl.bocadopolar.marketing.repository.CuponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final CuponRepository cuponRepository;
    private final CanalCampanaRepository canalRepository;
    private final CarritoAbandonadoRepository abandonadoRepository;

    @Override
    public void run(String... args) {
        if (cuponRepository.count() == 0) {
            cuponRepository.saveAll(List.of(
                    cupon("BIENVENIDA10", TipoDescuento.PORCENTAJE, 10, 100, 12, true, 60),
                    cupon("POLAR5000", TipoDescuento.MONTO_FIJO, 5000, 50, 3, true, 30),
                    cupon("VERANO20", TipoDescuento.PORCENTAJE, 20, 30, 30, false, -5)));
        }
        if (canalRepository.count() == 0) {
            canalRepository.saveAll(List.of(
                    new CanalCampana(null, "Directo", 640, 14, new BigDecimal("612000")),
                    new CanalCampana(null, "Instagram", 420, 9, new BigDecimal("395000")),
                    new CanalCampana(null, "Anuncios (Meta)", 310, 6, new BigDecimal("268000")),
                    new CanalCampana(null, "Búsqueda Google", 280, 5, new BigDecimal("231000")),
                    new CanalCampana(null, "Correo / campaña", 120, 4, new BigDecimal("182000"))));
        }
        if (abandonadoRepository.count() == 0) {
            abandonadoRepository.saveAll(List.of(
                    abandonado("Camila Rojas", "camila.rojas@correo.cl", "Salmón Premium · 3 kg", 38970, 2),
                    abandonado("Pedro Araya", "pedro.araya@correo.cl", "Pulpo Cocido · 2 kg", 29980, 24),
                    abandonado("Visitante", null, "Mix Mariscos · 1,5 kg", 17985, 72)));
        }
        log.info("ms-marketing: datos de ejemplo verificados");
    }

    private Cupon cupon(String codigo, TipoDescuento tipo, int valor, int limite, int usos, boolean activo, int dias) {
        Cupon c = new Cupon();
        c.setCodigo(codigo);
        c.setTipo(tipo);
        c.setValor(BigDecimal.valueOf(valor));
        c.setCompraMinima(BigDecimal.ZERO);
        c.setLimiteUsos(limite);
        c.setUsos(usos);
        c.setLimitePorCliente(1);
        c.setActivo(activo);
        c.setVence(LocalDate.now().plusDays(dias));
        return c;
    }

    private CarritoAbandonado abandonado(String cliente, String email, String resumen, int total, int horas) {
        CarritoAbandonado a = new CarritoAbandonado();
        a.setCliente(cliente);
        a.setEmail(email);
        a.setResumen(resumen);
        a.setTotal(BigDecimal.valueOf(total));
        a.setFecha(Instant.now().minus(horas, ChronoUnit.HOURS));
        return a;
    }
}
