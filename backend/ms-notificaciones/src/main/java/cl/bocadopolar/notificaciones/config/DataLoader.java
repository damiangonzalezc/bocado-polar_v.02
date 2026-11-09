package cl.bocadopolar.notificaciones.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.notificaciones.model.MotivoNotificacion;
import cl.bocadopolar.notificaciones.model.PlantillaMensaje;
import cl.bocadopolar.notificaciones.repository.PlantillaMensajeRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final PlantillaMensajeRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                p(MotivoNotificacion.PEDIDO_CONFIRMADO, "Recibimos tu pedido {{codigo}}",
                        "Hola {{cliente}}:\n\nRecibimos tu pedido {{codigo}} por ${{total}}.\n"
                                + "Entrega: {{fechaEntrega}} entre {{horario}}.\n\nGracias por comprar en BOCADO Polar."),
                p(MotivoNotificacion.CAMBIO_ESTADO, "Tu pedido {{codigo}} ahora está {{estado}}",
                        "Hola {{cliente}}:\n\nTu pedido {{codigo}} cambió de {{estadoAnterior}} a {{estado}}.\n"
                                + "Puedes seguirlo en Mis pedidos."),
                p(MotivoNotificacion.ALERTA_FRIO, "Alerta de cadena de frío ({{severidad}})", "{{detalle}}"),
                p(MotivoNotificacion.ALERTA_STOCK, "Stock crítico: {{producto}}",
                        "{{producto}} tiene {{kg}} kg disponibles (umbral {{umbral}} kg)."),
                p(MotivoNotificacion.INCIDENCIA_REPARTO, "Incidencia de reparto: {{tipo}}",
                        "{{repartidor}} reportó {{tipo}} en el pedido {{pedidoId}}: {{nota}}"),
                p(MotivoNotificacion.RESCATE_CARRITO, "{{cliente}}, dejaste productos en tu carrito",
                        "Hola {{cliente}}:\n\nTu carrito te espera: {{resumen}} (total ${{total}}).\n"
                                + "Usa el cupón {{cupon}} y completa tu compra en BOCADO Polar.")));
    }

    private PlantillaMensaje p(MotivoNotificacion motivo, String asunto, String cuerpo) {
        return new PlantillaMensaje(null, motivo, asunto, cuerpo, true);
    }
}
