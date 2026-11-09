package cl.bocadopolar.rutas.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.rutas.client.Clientes.PedidoDto;
import cl.bocadopolar.rutas.client.Clientes.PedidosClient;
import cl.bocadopolar.rutas.client.Clientes.RepartoClient;
import cl.bocadopolar.rutas.client.Remoto;
import cl.bocadopolar.rutas.dto.RutaDtos.ParadaRequest;
import cl.bocadopolar.rutas.dto.RutaDtos.RutaRequest;
import cl.bocadopolar.rutas.dto.RutaDtos.VehiculoRequest;
import cl.bocadopolar.rutas.exception.RecursoNoEncontradoException;
import cl.bocadopolar.rutas.exception.ReglaNegocioException;
import cl.bocadopolar.rutas.model.EstadoParada;
import cl.bocadopolar.rutas.model.EstadoRuta;
import cl.bocadopolar.rutas.model.ParadaRuta;
import cl.bocadopolar.rutas.model.Ruta;
import cl.bocadopolar.rutas.model.Vehiculo;
import cl.bocadopolar.rutas.repository.ParadaRutaRepository;
import cl.bocadopolar.rutas.repository.RutaRepository;
import cl.bocadopolar.rutas.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RutaService {

    /** Coordenadas aproximadas por comuna (igual que repartidor.js). La bodega está en Concepción. */
    static final Map<String, double[]> COORDENADAS = Map.of(
            "Concepción", new double[] { -36.827, -73.050 },
            "Chiguayante", new double[] { -36.925, -73.029 },
            "Penco", new double[] { -36.741, -72.997 },
            "Tomé", new double[] { -36.617, -72.957 },
            "Talcahuano", new double[] { -36.724, -73.117 },
            "San Pedro de la Paz", new double[] { -36.843, -73.108 },
            "Hualpén", new double[] { -36.792, -73.094 });
    static final double[] BODEGA = COORDENADAS.get("Concepción");
    private static final Set<String> ESTADOS_DESPACHABLES = Set.of("EN_PREPARACION", "ENVIADO");

    private final RutaRepository rutaRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ParadaRutaRepository paradaRepository;
    private final PedidosClient pedidos;
    private final RepartoClient reparto;

    /* =============== Rutas =============== */

    public List<Ruta> listar(LocalDate fecha, EstadoRuta estado, Long repartidorId) {
        return rutaRepository.findAll().stream()
                .filter(r -> fecha == null || fecha.equals(r.getFecha()))
                .filter(r -> estado == null || estado == r.getEstado())
                .filter(r -> repartidorId == null || repartidorId.equals(r.getRepartidorId()))
                .sorted(Comparator.comparing(Ruta::getFecha).reversed().thenComparing(Ruta::getHoraSalida))
                .toList();
    }

    public Ruta obtener(Long id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la ruta " + id));
    }

    @Transactional
    public Ruta crear(RutaRequest req) {
        Ruta r = new Ruta();
        r.setCodigo("TMP-" + System.nanoTime() % 100000000);
        copiar(req, r);
        r = rutaRepository.save(r);
        r.setCodigo(String.format("R-%03d", r.getId()));
        return rutaRepository.save(r);
    }

    @Transactional
    public Ruta actualizar(Long id, RutaRequest req) {
        Ruta r = obtener(id);
        exigirEditable(r);
        copiar(req, r);
        return rutaRepository.save(r);
    }

    @Transactional
    public void eliminar(Long id) {
        Ruta r = obtener(id);
        if (r.getEstado() == EstadoRuta.EN_CURSO) {
            throw new ReglaNegocioException("No se puede eliminar una ruta en curso; finalízala o cancélala primero");
        }
        rutaRepository.delete(r);
    }

    @Transactional
    public Ruta asignar(Long id, Long repartidorId, String repartidorNombre, Long vehiculoId) {
        Ruta r = obtener(id);
        exigirEditable(r);
        r.setRepartidorId(repartidorId);
        r.setRepartidorNombre(nombreRepartidor(repartidorId, repartidorNombre));
        r.setVehiculo(vehiculoActivo(vehiculoId));
        return rutaRepository.save(r);
    }

    /** Al salir: pasa a EN_CURSO y marca como ENVIADO cada pedido pendiente de la ruta. */
    @Transactional
    public Ruta iniciar(Long id) {
        Ruta r = obtener(id);
        if (r.getEstado() == EstadoRuta.EN_CURSO) {
            return r;
        }
        if (r.getEstado() != EstadoRuta.PLANIFICADA) {
            throw new ReglaNegocioException("Solo se inicia una ruta PLANIFICADA (esta está " + r.getEstado() + ")");
        }
        if (r.getRepartidorId() == null || r.getVehiculo() == null) {
            throw new ReglaNegocioException("Asigna repartidor y vehículo antes de iniciar la ruta");
        }
        r.setEstado(EstadoRuta.EN_CURSO);
        r.setIniciadaEn(Instant.now());
        r.getParadas().stream().filter(p -> p.getEstado() == EstadoParada.PENDIENTE)
                .forEach(p -> pedidos.marcarEnviado(p.getPedidoId(), r.getRepartidorId()));
        return rutaRepository.save(r);
    }

    @Transactional
    public Ruta finalizar(Long id) {
        Ruta r = obtener(id);
        if (r.getEstado() != EstadoRuta.EN_CURSO) {
            throw new ReglaNegocioException("Solo se finaliza una ruta EN_CURSO");
        }
        r.setEstado(EstadoRuta.FINALIZADA);
        r.setFinalizadaEn(Instant.now());
        return rutaRepository.save(r);
    }

    @Transactional
    public Ruta cancelar(Long id) {
        Ruta r = obtener(id);
        if (r.getEstado() == EstadoRuta.FINALIZADA) {
            throw new ReglaNegocioException("La ruta ya finalizó");
        }
        r.setEstado(EstadoRuta.CANCELADA);
        return rutaRepository.save(r);
    }

    /** CU-08.06 La ruta de hoy del repartidor (primero la que está en curso). */
    public Ruta rutaDelDia(Long repartidorId) {
        return rutaRepository.findByRepartidorIdAndFecha(repartidorId, LocalDate.now()).stream()
                .filter(r -> r.getEstado() == EstadoRuta.EN_CURSO || r.getEstado() == EstadoRuta.PLANIFICADA)
                .min(Comparator.comparing((Ruta r) -> r.getEstado() == EstadoRuta.EN_CURSO ? 0 : 1)
                        .thenComparing(Ruta::getHoraSalida))
                .orElseThrow(() -> new RecursoNoEncontradoException("El repartidor " + repartidorId + " no tiene ruta para hoy"));
    }

    /* =============== Paradas =============== */

    /** Agrega como paradas los pedidos listos para despacho de las comunas y fecha de la ruta. */
    @Transactional
    public Ruta generarParadas(Long id) {
        Ruta r = obtener(id);
        exigirEditableOEnCurso(r);
        Set<Long> yaIncluidos = r.getParadas().stream().map(ParadaRuta::getPedidoId).collect(Collectors.toSet());
        Set<Long> enOtrasRutas = rutaRepository.findByFecha(r.getFecha()).stream()
                .filter(o -> !o.getId().equals(r.getId()) && o.getEstado() != EstadoRuta.CANCELADA)
                .flatMap(o -> o.getParadas().stream()).map(ParadaRuta::getPedidoId).collect(Collectors.toSet());
        for (PedidoDto p : pedidos.pedidosPara(r.getComunas(), r.getFecha())) {
            boolean deOtroRepartidor = p.repartidorId() != null && !p.repartidorId().equals(r.getRepartidorId());
            if (!ESTADOS_DESPACHABLES.contains(p.estado()) || yaIncluidos.contains(p.id()) || enOtrasRutas.contains(p.id())
                    || deOtroRepartidor) {
                continue; // un pedido asignado por el admin a otro repartidor no se mueve
            }
            if (r.getRepartidorId() != null && p.repartidorId() == null) {
                Long rid = r.getRepartidorId();
                String rnom = r.getRepartidorNombre();
                Remoto.intentar("ms-pedidos", () -> pedidos.asignarRepartidor(p.id(), rid, rnom, null));
            }
            ParadaRuta parada = new ParadaRuta();
            parada.setPedidoId(p.id());
            parada.setCodigoPedido(p.codigo());
            parada.setCliente(p.nombreCliente());
            parada.setTelefono(p.telefono());
            parada.setDireccion(p.direccion());
            parada.setComuna(p.comuna());
            parada.setReferencia(p.referencia());
            parada.setHorario(p.horario());
            boolean cobrar = "CONTRA_ENTREGA".equals(p.metodoPago());
            parada.setCobrarAlEntregar(cobrar);
            parada.setMontoACobrar(cobrar ? p.total() : null);
            parada.setOrden(r.getParadas().size() + 1);
            r.getParadas().add(parada);
        }
        ordenarPorCercania(r);
        rutaRepository.flush();
        return r;
    }

    @Transactional
    public Ruta asignarPedido(Long pedidoId, Long repartidorId, String repartidorNombre, Long usuarioId) {
        PedidoDto pedido = pedidos.obtener(pedidoId);
        String nombre = nombreRepartidor(repartidorId, repartidorNombre);
        LocalDate fecha = pedido.fechaEntrega() == null ? LocalDate.now() : pedido.fechaEntrega();

        // 1. Si ya estaba en otra ruta, se saca de ahí (solo si esa parada sigue pendiente).
        for (Ruta otra : rutaRepository.findByFecha(fecha)) {
            if (otra.getEstado() == EstadoRuta.CANCELADA || repartidorId.equals(otra.getRepartidorId())) {
                continue;
            }
            otra.getParadas().stream().filter(x -> x.getPedidoId().equals(pedidoId)).findFirst().ifPresent(x -> {
                if (x.getEstado() != EstadoParada.PENDIENTE || otra.getEstado() == EstadoRuta.FINALIZADA) {
                    throw new ReglaNegocioException("El pedido " + pedido.codigo() + " ya fue gestionado en la ruta "
                            + otra.getCodigo() + "; no se puede reasignar");
                }
                otra.getParadas().remove(x);
                for (int i = 0; i < otra.getParadas().size(); i++) {
                    otra.getParadas().get(i).setOrden(i + 1);
                }
            });
        }

        // 2. Ruta del repartidor para esa fecha (la crea si no tiene).
        Ruta r = rutaRepository.findByRepartidorIdAndFecha(repartidorId, fecha).stream()
                .filter(x -> x.getEstado() == EstadoRuta.PLANIFICADA || x.getEstado() == EstadoRuta.EN_CURSO)
                .min(Comparator.comparing((Ruta x) -> x.getEstado() == EstadoRuta.EN_CURSO ? 0 : 1))
                .orElseGet(() -> nuevaRutaPara(repartidorId, nombre, fecha));

        // 3. ms-pedidos valida el estado y guarda el repartidor (si falla, no se toca nada).
        PedidoDto asignado = pedidos.asignarRepartidor(pedidoId, repartidorId, nombre, usuarioId);

        if (!r.getComunas().contains(pedido.comuna())) {
            r.getComunas().add(pedido.comuna());
        }
        if (r.getParadas().stream().noneMatch(x -> x.getPedidoId().equals(pedidoId))) {
            ParadaRuta parada = new ParadaRuta();
            parada.setPedidoId(pedido.id());
            parada.setCodigoPedido(pedido.codigo());
            parada.setCliente(pedido.nombreCliente());
            parada.setTelefono(pedido.telefono());
            parada.setDireccion(pedido.direccion());
            parada.setComuna(pedido.comuna());
            parada.setReferencia(pedido.referencia());
            parada.setHorario(pedido.horario());
            boolean cobrar = "CONTRA_ENTREGA".equals(pedido.metodoPago());
            parada.setCobrarAlEntregar(cobrar);
            parada.setMontoACobrar(cobrar ? pedido.total() : null);
            parada.setOrden(r.getParadas().size() + 1);
            r.getParadas().add(parada);
        }
        ordenarPorCercania(r);
        Ruta guardada = rutaRepository.save(r);
        if (guardada.getEstado() == EstadoRuta.EN_CURSO && "EN_PREPARACION".equals(asignado.estado())) {
            pedidos.marcarEnviado(pedidoId, repartidorId);
        }
        return guardada;
    }

    private Ruta nuevaRutaPara(Long repartidorId, String nombre, LocalDate fecha) {
        Ruta r = new Ruta();
        r.setCodigo("TMP-" + System.nanoTime() % 100000000);
        r.setNombre("Ruta de " + (nombre == null ? "repartidor " + repartidorId : nombre));
        r.setFecha(fecha);
        r.setHoraSalida(java.time.LocalTime.of(8, 30));
        r.setRepartidorId(repartidorId);
        r.setRepartidorNombre(nombre);
        // vehículo refrigerado activo que no esté ocupado ese día (el admin puede cambiarlo después)
        Set<Long> ocupados = rutaRepository.findByFecha(fecha).stream()
                .filter(x -> x.getVehiculo() != null && x.getEstado() != EstadoRuta.CANCELADA)
                .map(x -> x.getVehiculo().getId()).collect(Collectors.toSet());
        vehiculoRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getActivo()) && Boolean.TRUE.equals(v.getRefrigerado())
                        && !ocupados.contains(v.getId()))
                .findFirst().ifPresent(r::setVehiculo);
        r = rutaRepository.save(r);
        r.setCodigo(String.format("R-%03d", r.getId()));
        return r;
    }

    @Transactional
    public ParadaRuta agregarParada(Long id, ParadaRequest req) {
        Ruta r = obtener(id);
        exigirEditableOEnCurso(r);
        if (r.getParadas().stream().anyMatch(p -> p.getPedidoId().equals(req.pedidoId()))) {
            throw new ReglaNegocioException("El pedido " + req.pedidoId() + " ya está en la ruta");
        }
        ParadaRuta p = new ParadaRuta();
        p.setPedidoId(req.pedidoId());
        p.setCodigoPedido(req.codigoPedido());
        p.setCliente(req.cliente());
        p.setDireccion(req.direccion());
        p.setComuna(req.comuna());
        p.setReferencia(req.referencia());
        p.setHorario(req.horario());
        p.setCobrarAlEntregar(Boolean.TRUE.equals(req.cobrarAlEntregar()));
        p.setMontoACobrar(req.montoACobrar());
        p.setOrden(r.getParadas().size() + 1);
        r.getParadas().add(p);
        rutaRepository.flush();
        return p;
    }

    @Transactional
    public void quitarParada(Long id, Long paradaId) {
        Ruta r = obtener(id);
        exigirEditableOEnCurso(r);
        if (!r.getParadas().removeIf(p -> p.getId().equals(paradaId))) {
            throw new RecursoNoEncontradoException("La ruta no tiene la parada " + paradaId);
        }
        for (int i = 0; i < r.getParadas().size(); i++) {
            r.getParadas().get(i).setOrden(i + 1);
        }
        rutaRepository.save(r);
    }

    /** CU-08.05 Reordena las paradas pendientes: siempre la comuna más cercana a la anterior (desde la bodega). */
    @Transactional
    public Ruta optimizar(Long id) {
        Ruta r = obtener(id);
        ordenarPorCercania(r);
        return rutaRepository.save(r);
    }

    @Transactional
    public ParadaRuta marcarEntregada(Long paradaId) {
        ParadaRuta p = parada(paradaId);
        p.setEstado(EstadoParada.ENTREGADA);
        p.setMotivoFalla(null);
        p.setCerradaEn(Instant.now());
        return paradaRepository.save(p);
    }

    @Transactional
    public ParadaRuta marcarFallida(Long paradaId, String motivo) {
        ParadaRuta p = parada(paradaId);
        p.setEstado(EstadoParada.FALLIDA);
        p.setMotivoFalla(motivo);
        p.setCerradaEn(Instant.now());
        return paradaRepository.save(p);
    }

    public ParadaRuta parada(Long paradaId) {
        return paradaRepository.findById(paradaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la parada " + paradaId));
    }

    /* =============== Vehículos =============== */

    public List<Vehiculo> vehiculos() {
        return vehiculoRepository.findAll();
    }

    public Vehiculo vehiculo(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el vehículo " + id));
    }

    @Transactional
    public Vehiculo crearVehiculo(VehiculoRequest req) {
        String patente = req.patente().trim().toUpperCase();
        if (vehiculoRepository.findByPatenteIgnoreCase(patente).isPresent()) {
            throw new ReglaNegocioException("Ya existe un vehículo con patente " + patente);
        }
        return vehiculoRepository.save(new Vehiculo(null, req.tipo().trim(), patente,
                req.refrigerado() == null || req.refrigerado(), req.activo() == null || req.activo()));
    }

    @Transactional
    public Vehiculo actualizarVehiculo(Long id, VehiculoRequest req) {
        Vehiculo v = vehiculo(id);
        String patente = req.patente().trim().toUpperCase();
        vehiculoRepository.findByPatenteIgnoreCase(patente).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new ReglaNegocioException("Ya existe un vehículo con patente " + patente);
        });
        v.setTipo(req.tipo().trim());
        v.setPatente(patente);
        if (req.refrigerado() != null) {
            v.setRefrigerado(req.refrigerado());
        }
        if (req.activo() != null) {
            v.setActivo(req.activo());
        }
        return vehiculoRepository.save(v);
    }

    @Transactional
    public void eliminarVehiculo(Long id) {
        Vehiculo v = vehiculo(id);
        boolean enUso = rutaRepository.findAll().stream()
                .anyMatch(r -> r.getVehiculo() != null && r.getVehiculo().getId().equals(id)
                        && (r.getEstado() == EstadoRuta.PLANIFICADA || r.getEstado() == EstadoRuta.EN_CURSO));
        if (enUso) {
            throw new ReglaNegocioException("El vehículo está asignado a una ruta activa");
        }
        vehiculoRepository.delete(v);
    }

    /* =============== internos =============== */

    private void copiar(RutaRequest req, Ruta r) {
        r.setNombre(req.nombre().trim());
        r.setFecha(req.fecha());
        r.getComunas().clear();
        r.getComunas().addAll(req.comunas().stream().map(String::trim).distinct().toList());
        r.setHoraSalida(req.horaSalida());
        r.setRepartidorId(req.repartidorId());
        r.setRepartidorNombre(req.repartidorId() == null ? null : nombreRepartidor(req.repartidorId(), req.repartidorNombre()));
        r.setVehiculo(req.vehiculoId() == null ? null : vehiculoActivo(req.vehiculoId()));
        r.setNotas(req.notas());
    }

    private String nombreRepartidor(Long repartidorId, String nombreEnviado) {
        if (nombreEnviado != null && !nombreEnviado.isBlank()) {
            return nombreEnviado.trim();
        }
        return reparto.nombre(repartidorId);
    }

    private Vehiculo vehiculoActivo(Long vehiculoId) {
        Vehiculo v = vehiculo(vehiculoId);
        if (!Boolean.TRUE.equals(v.getActivo())) {
            throw new ReglaNegocioException("El vehículo " + v.getPatente() + " no está activo");
        }
        return v;
    }

    private void exigirEditable(Ruta r) {
        if (r.getEstado() != EstadoRuta.PLANIFICADA) {
            throw new ReglaNegocioException("Solo se edita una ruta PLANIFICADA (esta está " + r.getEstado() + ")");
        }
    }

    private void exigirEditableOEnCurso(Ruta r) {
        if (r.getEstado() != EstadoRuta.PLANIFICADA && r.getEstado() != EstadoRuta.EN_CURSO) {
            throw new ReglaNegocioException("La ruta está " + r.getEstado());
        }
    }

    void ordenarPorCercania(Ruta r) {
        List<ParadaRuta> cerradas = r.getParadas().stream().filter(p -> p.getEstado() != EstadoParada.PENDIENTE).toList();
        List<ParadaRuta> resto = new ArrayList<>(r.getParadas().stream().filter(p -> p.getEstado() == EstadoParada.PENDIENTE).toList());
        List<ParadaRuta> orden = new ArrayList<>(cerradas);
        double[] punto = BODEGA;
        while (!resto.isEmpty()) {
            final double[] desde = punto;
            ParadaRuta siguiente = resto.stream()
                    .min(Comparator.comparingDouble((ParadaRuta p) -> distancia(desde, coord(p.getComuna())))
                            .thenComparing(p -> p.getHorario() == null ? "" : p.getHorario()))
                    .orElseThrow();
            resto.remove(siguiente);
            orden.add(siguiente);
            punto = coord(siguiente.getComuna());
        }
        for (int i = 0; i < orden.size(); i++) {
            orden.get(i).setOrden(i + 1);
        }
    }

    private static double[] coord(String comuna) {
        return COORDENADAS.getOrDefault(comuna, BODEGA);
    }

    private static double distancia(double[] a, double[] b) {
        double dLat = a[0] - b[0];
        double dLng = a[1] - b[1];
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
}
