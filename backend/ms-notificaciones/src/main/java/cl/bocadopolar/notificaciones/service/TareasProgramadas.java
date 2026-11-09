package cl.bocadopolar.notificaciones.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TareasProgramadas {

    private final NotificacionService service;

    /** Cada 5 minutos reintenta las notificaciones fallidas (máximo 3 intentos). */
    @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
    public void reintentar() {
        try {
            int n = service.reintentarFallidas();
            if (n > 0) {
                log.info("Notificaciones reintentadas: {}", n);
            }
        } catch (Exception e) {
            log.warn("Error al reintentar notificaciones: {}", e.getMessage());
        }
    }
}
