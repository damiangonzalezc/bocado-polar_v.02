package cl.bocadopolar.rutas.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.bocadopolar.rutas.model.EstadoParada;
import cl.bocadopolar.rutas.model.EstadoRuta;
import cl.bocadopolar.rutas.model.ParadaRuta;
import cl.bocadopolar.rutas.model.Ruta;
import cl.bocadopolar.rutas.model.Vehiculo;
import cl.bocadopolar.rutas.repository.RutaRepository;
import cl.bocadopolar.rutas.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Rutas de hoy del prototipo: R-001 (Concepción → Chiguayante, repartidor usuario 5) en curso y
 * R-002 (Tomé → Penco, repartidor usuario 6) planificada. Las paradas apuntan a los pedidos 1..7 de ms-pedidos.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bocado.semilla.habilitada", havingValue = "true", matchIfMissing = true)
public class DataLoader implements CommandLineRunner {

    private final RutaRepository rutaRepository;
    private final VehiculoRepository vehiculoRepository;

    @Override
    public void run(String... args) {
        if (vehiculoRepository.count() == 0) {
            vehiculoRepository.saveAll(List.of(
                    new Vehiculo(null, "Camión refrigerado", "KLTY-21", true, true),
                    new Vehiculo(null, "Furgón refrigerado", "PLXD-48", true, true)));
        }
        if (rutaRepository.count() > 0) {
            return;
        }
        Vehiculo camion = vehiculoRepository.findByPatenteIgnoreCase("KLTY-21").orElseThrow();
        Vehiculo furgon = vehiculoRepository.findByPatenteIgnoreCase("PLXD-48").orElseThrow();

        Ruta r1 = ruta("R-001", "Concepción → Chiguayante", List.of("Concepción", "Chiguayante"), "08:30", 5L,
                "Repartidor Demo", camion, EstadoRuta.EN_CURSO, "Salida desde bodega central.");
        r1.setIniciadaEn(Instant.now().minusSeconds(3600));
        r1.getParadas().add(parada(1L, "BP-100001", "Cliente Demo", "O'Higgins 1234", "Concepción", "Tocar timbre 3B", "09:00 - 12:00", null, 1));
        r1.getParadas().add(parada(2L, "BP-100002", "Restaurante Mar Azul (B2B)", "Los Carrera 455", "Chiguayante",
                "Entrada de proveedores, portón azul", "12:00 - 15:00", new BigDecimal("183370"), 2));
        rutaRepository.save(r1);

        Ruta r2 = ruta("R-002", "Tomé → Penco", List.of("Tomé", "Penco"), "09:15", 6L, "Rodrigo Norte", furgon,
                EstadoRuta.PLANIFICADA, "Cargar bolsas térmicas extra.");
        r2.getParadas().add(parada(7L, "BP-100007", "Cevichería El Faro", "Costanera 12", "Penco", null, "12:00 - 15:00", null, 1));
        r2.getParadas().add(parada(3L, "BP-100003", "Cliente Demo", "Ejército 88", "Tomé", "Dejar en conserjería", "15:00 - 18:00", null, 2));
        ParadaRuta entregada = parada(6L, "BP-100006", "Camila Rojas", "Sotomayor 45", "Tomé", null, "09:00 - 12:00", null, 3);
        entregada.setEstado(EstadoParada.ENTREGADA);
        entregada.setCerradaEn(Instant.now().minusSeconds(5400));
        r2.getParadas().add(entregada);
        rutaRepository.save(r2);
        log.info("ms-rutas: rutas de ejemplo creadas");
    }

    private Ruta ruta(String codigo, String nombre, List<String> comunas, String salida, Long repartidorId,
            String repartidorNombre, Vehiculo v, EstadoRuta estado, String notas) {
        Ruta r = new Ruta();
        r.setCodigo(codigo);
        r.setNombre(nombre);
        r.setFecha(LocalDate.now());
        r.getComunas().addAll(comunas);
        r.setHoraSalida(LocalTime.parse(salida));
        r.setRepartidorId(repartidorId);
        r.setRepartidorNombre(repartidorNombre);
        r.setVehiculo(v);
        r.setEstado(estado);
        r.setNotas(notas);
        return r;
    }

    private ParadaRuta parada(Long pedidoId, String codigo, String cliente, String direccion, String comuna,
            String referencia, String horario, BigDecimal cobrar, int orden) {
        ParadaRuta p = new ParadaRuta();
        p.setPedidoId(pedidoId);
        p.setCodigoPedido(codigo);
        p.setCliente(cliente);
        p.setDireccion(direccion);
        p.setComuna(comuna);
        p.setReferencia(referencia);
        p.setHorario(horario);
        p.setCobrarAlEntregar(cobrar != null);
        p.setMontoACobrar(cobrar);
        p.setOrden(orden);
        return p;
    }
}
