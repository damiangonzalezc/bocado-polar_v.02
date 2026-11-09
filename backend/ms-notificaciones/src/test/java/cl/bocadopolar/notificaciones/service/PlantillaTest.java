package cl.bocadopolar.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PlantillaTest {

    @Test
    void reemplazaLasVariables() {
        String r = NotificacionService.renderizar("Hola {{cliente}}, tu pedido {{codigo}} vale ${{total}}",
                Map.of("cliente", "Ana", "codigo", "BP-100001", "total", 48455));
        assertThat(r).isEqualTo("Hola Ana, tu pedido BP-100001 vale $48455");
    }

    @Test
    void variableFaltanteQuedaVacia() {
        assertThat(NotificacionService.renderizar("Cupón: {{cupon}}", Map.of())).isEqualTo("Cupón: ");
    }
}
