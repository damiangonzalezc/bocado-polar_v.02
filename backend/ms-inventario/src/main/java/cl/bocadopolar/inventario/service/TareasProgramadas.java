package cl.bocadopolar.inventario.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TareasProgramadas {

    private final InventarioService service;

    /** Cada minuto libera las reservas de pedidos que no se pagaron a tiempo. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void expirarReservas() {
        try {
            int n = service.expirarReservas();
            if (n > 0) {
                log.info("Reservas expiradas: {}", n);
            }
        } catch (Exception e) {
            log.warn("No se pudieron expirar reservas: {}", e.getMessage());
        }
    }
}
