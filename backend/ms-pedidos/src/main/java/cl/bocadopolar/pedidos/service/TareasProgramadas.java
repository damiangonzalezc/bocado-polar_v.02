package cl.bocadopolar.pedidos.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TareasProgramadas {

    private final PedidoService service;

    /** Cada minuto cancela los pedidos que no se pagaron a tiempo. */
    @Scheduled(fixedDelay = 60_000, initialDelay = 45_000)
    public void cancelarVencidos() {
        try {
            int n = service.cancelarVencidos();
            if (n > 0) {
                log.info("Pedidos cancelados por falta de pago: {}", n);
            }
        } catch (Exception e) {
            log.warn("No se pudieron revisar pedidos vencidos: {}", e.getMessage());
        }
    }
}
