package cl.bocadopolar.clientes.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.clientes.model.Cliente;
import cl.bocadopolar.clientes.model.DireccionEntrega;
import cl.bocadopolar.clientes.model.MedioPago;
import cl.bocadopolar.clientes.model.NotaCliente;
import cl.bocadopolar.clientes.model.SegmentoCliente;
import cl.bocadopolar.clientes.model.TipoNota;
import cl.bocadopolar.clientes.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Cliente Demo (id 1, usuario 7 de ms-auth) y clientes del historial del prototipo. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final ClienteRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        Cliente demo = cliente(7L, "Cliente", "Demo", "cliente@bocado.cl", "+56 9 4444 4444", "11111111-1",
                SegmentoCliente.FRECUENTE, "289000", 6);
        demo.getDirecciones().add(new DireccionEntrega(null, "Casa", "O'Higgins 1234", "Concepción", "Tocar timbre 3B", true));
        demo.getDirecciones().add(new DireccionEntrega(null, "Trabajo", "Ejército 88", "Tomé", "Dejar en conserjería", false));
        demo.getMediosPago().add(new MedioPago(null, "Mi Visa", "Visa", "1111", "tok_demo_visa_1111", "08/29"));
        demo.getPreferencia().setNotas("Prefiero entregas en la mañana.");
        repository.save(demo);

        Cliente maria = cliente(null, "María", "Soto", "maria.soto@correo.cl", "+56 9 5555 1002", null,
                SegmentoCliente.FRECUENTE, "152000", 4);
        maria.getDirecciones().add(new DireccionEntrega(null, "Casa", "Los Aromos 321", "Chiguayante", null, true));
        maria.getNotas().add(new NotaCliente(null, TipoNota.RECLAMO,
                "Reporta bolsa de camarones con escarcha. Se ofreció cambio.", 4L, Instant.now().minus(6, ChronoUnit.DAYS)));
        repository.save(maria);

        guardar(cliente(null, "Pedro", "Araya", "pedro.araya@correo.cl", null, null, SegmentoCliente.NUEVO, "64000", 2),
                "Av. Collao 1550", "Concepción");
        guardar(cliente(null, "Camila", "Rojas", "camila.rojas@correo.cl", null, null, SegmentoCliente.NUEVO, "38000", 1),
                "Sotomayor 45", "Tomé");
        guardar(cliente(null, "Restaurante Mar Azul (B2B)", null, "ventas@marazul.cl", null, null, SegmentoCliente.VIP,
                "820000", 11), "Los Carrera 455", "Chiguayante");
        guardar(cliente(null, "Cevichería El Faro", null, "compras@elfaro.cl", null, null, SegmentoCliente.FRECUENTE,
                "310000", 5), "Costanera 12", "Penco");
        log.info("ms-clientes: datos de ejemplo creados");
    }

    private void guardar(Cliente c, String calle, String comuna) {
        c.getDirecciones().add(new DireccionEntrega(null, "Principal", calle, comuna, null, true));
        repository.save(c);
    }

    private Cliente cliente(Long usuarioId, String nombre, String apellido, String email, String tel, String rut,
            SegmentoCliente segmento, String gasto, int pedidos) {
        Cliente c = new Cliente();
        c.setUsuarioId(usuarioId);
        c.setNombre(nombre);
        c.setApellido(apellido);
        c.setEmail(email);
        c.setTelefono(tel);
        c.setRut(rut);
        c.setSegmento(segmento);
        c.setGastoTotal(new BigDecimal(gasto));
        c.setTotalPedidos(pedidos);
        c.setCreadoEn(Instant.now());
        return c;
    }
}
