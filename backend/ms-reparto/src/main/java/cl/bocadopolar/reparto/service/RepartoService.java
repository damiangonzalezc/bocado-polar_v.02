package cl.bocadopolar.reparto.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.reparto.client.Clientes.CobroDto;
import cl.bocadopolar.reparto.client.Clientes.FrioClient;
import cl.bocadopolar.reparto.client.Clientes.NotificacionesClient;
import cl.bocadopolar.reparto.client.Clientes.PagoDto;
import cl.bocadopolar.reparto.client.Clientes.PagosClient;
import cl.bocadopolar.reparto.client.Clientes.PedidosClient;
import cl.bocadopolar.reparto.client.Clientes.RutaDto;
import cl.bocadopolar.reparto.client.Clientes.RutasClient;
import cl.bocadopolar.reparto.dto.RepartoDtos.EntregaFallidaRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.EntregaRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.IncidenciaRequest;
import cl.bocadopolar.reparto.dto.RepartoDtos.RepartidorRequest;
import cl.bocadopolar.reparto.exception.RecursoNoEncontradoException;
import cl.bocadopolar.reparto.exception.ReglaNegocioException;
import cl.bocadopolar.reparto.exception.SolicitudInvalidaException;
import cl.bocadopolar.reparto.model.CalificacionRepartidor;
import cl.bocadopolar.reparto.model.CierreCaja;
import cl.bocadopolar.reparto.model.Disponibilidad;
import cl.bocadopolar.reparto.model.Entrega;
import cl.bocadopolar.reparto.model.EstadoEntrega;
import cl.bocadopolar.reparto.model.EvidenciaEntrega;
import cl.bocadopolar.reparto.model.Incidencia;
import cl.bocadopolar.reparto.model.Repartidor;
import cl.bocadopolar.reparto.model.Turno;
import cl.bocadopolar.reparto.repository.CalificacionRepartidorRepository;
import cl.bocadopolar.reparto.repository.CierreCajaRepository;
import cl.bocadopolar.reparto.repository.EntregaRepository;
import cl.bocadopolar.reparto.repository.IncidenciaRepository;
import cl.bocadopolar.reparto.repository.RepartidorRepository;
import cl.bocadopolar.reparto.repository.TurnoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepartoService {

    private final RepartidorRepository repartidorRepository;
    private final TurnoRepository turnoRepository;
    private final EntregaRepository entregaRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final CierreCajaRepository cierreRepository;
    private final CalificacionRepartidorRepository calificacionRepository;
    private final RutasClient rutas;
    private final FrioClient frio;
    private final PagosClient pagos;
    private final PedidosClient pedidos;
    private final NotificacionesClient notificaciones;

    /* =============== Repartidores =============== */

    public List<Repartidor> listar(Disponibilidad disponibilidad) {
        return repartidorRepository.findAll().stream()
                .filter(r -> disponibilidad == null || r.getDisponibilidad() == disponibilidad)
                .toList();
    }

    public Repartidor obtener(Long id) {
        return repartidorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el repartidor " + id));
    }

    /** Lo llama ms-auth al crear un usuario REPARTIDOR (si ya existe, lo devuelve). */
    @Transactional
    public Repartidor registrar(RepartidorRequest req) {
        return repartidorRepository.findById(req.usuarioId()).orElseGet(() -> repartidorRepository.save(
                new Repartidor(req.usuarioId(), req.nombre().trim(), req.email(),
                        req.rut() == null || req.rut().isBlank() ? null : req.rut(),
                        Disponibilidad.DISPONIBLE, true, true, 0.0, 0)));
    }

    @Transactional
    public Repartidor cambiarDisponibilidad(Long id, Disponibilidad d) {
        Repartidor r = obtener(id);
        if (d != Disponibilidad.EN_RUTA && !turnoRepository.findByRepartidorIdAndFinIsNull(id).isEmpty()) {
            throw new ReglaNegocioException("Tienes un turno abierto: ciérralo antes de cambiar tu disponibilidad");
        }
        r.setDisponibilidad(d);
        return repartidorRepository.save(r);
    }

    @Transactional
    public Repartidor preferencias(Long id, boolean sonido, boolean vibracion) {
        Repartidor r = obtener(id);
        r.setSonido(sonido);
        r.setVibracion(vibracion);
        return repartidorRepository.save(r);
    }

    /* =============== Turnos =============== */

    /** CU-09.01 Iniciar turno: registra la temperatura de la caja, inicia la cuenta de 90 min y la ruta del día. */
    @Transactional
    public Turno iniciarTurno(Long repartidorId, Double tempCaja) {
        Repartidor r = obtener(repartidorId);
        if (!turnoRepository.findByRepartidorIdAndFinIsNull(repartidorId).isEmpty()) {
            throw new ReglaNegocioException("Ya tienes un turno abierto");
        }
        if (r.getDisponibilidad() == Disponibilidad.FUERA_SERVICIO) {
            r.setDisponibilidad(Disponibilidad.DISPONIBLE);
        }
        RutaDto ruta = rutas.rutaDelDia(repartidorId);
        Turno t = turnoRepository.save(new Turno(null, repartidorId, ruta == null ? null : ruta.id(), Instant.now(), null,
                tempCaja, false));
        Boolean alerta = frio.registrarTemperatura("SALIDA_RUTA", t.getId(), tempCaja, repartidorId);
        t.setAlertaTemperatura(Boolean.TRUE.equals(alerta));
        frio.iniciarCuentaRegresiva(t.getId());
        if (ruta != null) {
            rutas.accionRuta(ruta.id(), "iniciar");
        }
        r.setDisponibilidad(Disponibilidad.EN_RUTA);
        repartidorRepository.save(r);
        return turnoRepository.save(t);
    }

    public Turno turnoActivo(Long repartidorId) {
        return turnoRepository.findByRepartidorIdAndFinIsNull(repartidorId).stream().findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("El repartidor " + repartidorId + " no tiene turno abierto"));
    }

    /** CU-09.04 Cerrar turno y rendir caja con los cobros registrados en ms-pagos. */
    @Transactional
    public CierreCaja cerrarTurno(Long turnoId, BigDecimal efectivoRendido) {
        Turno t = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el turno " + turnoId));
        if (!t.activo()) {
            throw new ReglaNegocioException("El turno ya está cerrado");
        }
        LocalDate hoy = t.getInicio().atZone(ZoneId.systemDefault()).toLocalDate();
        BigDecimal efectivo = BigDecimal.ZERO;
        BigDecimal pos = BigDecimal.ZERO;
        BigDecimal transferencia = BigDecimal.ZERO;
        for (CobroDto c : pagos.cobrosDelDia(t.getRepartidorId(), hoy)) {
            BigDecimal monto = c.monto() == null ? BigDecimal.ZERO : c.monto();
            switch (c.tipo() == null ? "" : c.tipo()) {
                case "EFECTIVO" -> efectivo = efectivo.add(monto);
                case "POS_MOVIL" -> pos = pos.add(monto);
                case "TRANSFERENCIA" -> transferencia = transferencia.add(monto);
                default -> {
                    // tipo desconocido: no se suma
                }
            }
        }
        List<Entrega> entregas = entregaRepository.findByTurnoId(turnoId);
        int ok = (int) entregas.stream().filter(e -> e.getEstado() == EstadoEntrega.ENTREGADA).count();
        int fallidas = (int) entregas.stream().filter(e -> e.getEstado() == EstadoEntrega.FALLIDA).count();
        t.setFin(Instant.now());
        turnoRepository.save(t);
        Repartidor r = obtener(t.getRepartidorId());
        r.setDisponibilidad(Disponibilidad.DISPONIBLE);
        repartidorRepository.save(r);
        if (t.getRutaId() != null) {
            rutas.accionRuta(t.getRutaId(), "finalizar");
        }
        return cierreRepository.save(new CierreCaja(null, t.getRepartidorId(), turnoId, hoy, ok, fallidas, efectivo,
                efectivoRendido, efectivoRendido.subtract(efectivo), pos, transferencia, Instant.now()));
    }

    public List<CierreCaja> cierres(Long repartidorId) {
        return repartidorId == null ? cierreRepository.findAllByOrderByFechaDesc()
                : cierreRepository.findByRepartidorIdOrderByFechaDesc(repartidorId);
    }

    /* =============== Entregas =============== */

    /** CU-09.02 Registrar entrega con evidencia (foto o firma), temperatura y cobro si es contra entrega. */
    @Transactional
    public Entrega registrarEntrega(EntregaRequest req) {
        boolean sinFoto = req.fotoUrl() == null || req.fotoUrl().isBlank();
        boolean sinFirma = req.firmaUrl() == null || req.firmaUrl().isBlank();
        if (sinFoto && sinFirma) {
            throw new SolicitudInvalidaException("La entrega necesita evidencia: foto o firma del cliente");
        }
        obtener(req.repartidorId());
        Long turnoId = req.turnoId() != null ? req.turnoId()
                : turnoRepository.findByRepartidorIdAndFinIsNull(req.repartidorId()).stream().findFirst()
                        .map(Turno::getId).orElse(null);

        // 1) Cobro contra entrega pendiente
        PagoDto pendiente = pagos.pagosDelPedido(req.pedidoId()).stream()
                .filter(p -> "CONTRA_ENTREGA".equals(p.metodo()) && "PENDIENTE".equals(p.estado()))
                .findFirst().orElse(null);
        if (pendiente != null) {
            if (req.cobro() == null) {
                throw new SolicitudInvalidaException("Este pedido es contra entrega: falta registrar el cobro de $"
                        + pendiente.monto());
            }
            pagos.cobrar(pendiente.id(), req.repartidorId(), req.cobro().tipo(), req.cobro().pagaCon());
        }

        // 2) Pedido entregado (si ms-pedidos rechaza el cambio, no se registra nada)
        pedidos.marcarEntregado(req.pedidoId(), "Recibido por " + req.recibidoPor(), req.repartidorId());

        // 3) Temperatura de entrega y parada de la ruta
        if (req.tempEntrega() != null) {
            frio.registrarTemperatura("ENTREGA", req.pedidoId(), req.tempEntrega(), req.repartidorId());
        }
        if (req.paradaId() != null) {
            rutas.paradaEntregada(req.paradaId());
        }

        EvidenciaEntrega ev = new EvidenciaEntrega(null, sinFoto ? null : req.fotoUrl(), sinFirma ? null : req.firmaUrl());
        return entregaRepository.save(new Entrega(null, req.repartidorId(), turnoId, req.pedidoId(), req.paradaId(),
                EstadoEntrega.ENTREGADA, req.recibidoPor().trim(), req.tempEntrega(), req.comentario(), Instant.now(), ev));
    }

    /** CU-09.03 No se pudo entregar: queda la entrega fallida y su incidencia. */
    @Transactional
    public Entrega registrarFallida(EntregaFallidaRequest req) {
        obtener(req.repartidorId());
        Long turnoId = req.turnoId() != null ? req.turnoId()
                : turnoRepository.findByRepartidorIdAndFinIsNull(req.repartidorId()).stream().findFirst()
                        .map(Turno::getId).orElse(null);
        Entrega e = entregaRepository.save(new Entrega(null, req.repartidorId(), turnoId, req.pedidoId(), req.paradaId(),
                EstadoEntrega.FALLIDA, null, null, req.nota(), Instant.now(), null));
        crearIncidencia(req.tipo(), req.nota(), e.getId(), req.pedidoId(), req.repartidorId());
        if (req.paradaId() != null) {
            rutas.paradaFallida(req.paradaId(), req.tipo().name() + ": " + req.nota());
        }
        pedidos.agregarNota(req.pedidoId(), "Entrega fallida (" + req.tipo() + "): " + req.nota(), req.repartidorId());
        return e;
    }

    public List<Entrega> entregas(Long repartidorId, LocalDate fecha) {
        ZoneId zona = ZoneId.systemDefault();
        List<Entrega> base = repartidorId == null ? entregaRepository.findAll()
                : entregaRepository.findByRepartidorIdOrderByFechaDesc(repartidorId);
        return base.stream()
                .filter(e -> fecha == null || e.getFecha().atZone(zona).toLocalDate().equals(fecha))
                .toList();
    }

    public Entrega entrega(Long id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la entrega " + id));
    }

    public List<Entrega> entregasDelPedido(Long pedidoId) {
        return entregaRepository.findByPedidoIdOrderByFechaDesc(pedidoId);
    }

    /* =============== Incidencias =============== */

    @Transactional
    public Incidencia reportarIncidencia(IncidenciaRequest req) {
        return crearIncidencia(req.tipo(), req.nota(), req.entregaId(), req.pedidoId(), req.reportadaPorId());
    }

    public List<Incidencia> incidencias(Long repartidorId) {
        return repartidorId == null ? incidenciaRepository.findAllByOrderByFechaDesc()
                : incidenciaRepository.findByReportadaPorIdOrderByFechaDesc(repartidorId);
    }

    private Incidencia crearIncidencia(cl.bocadopolar.reparto.model.TipoIncidencia tipo, String nota, Long entregaId,
            Long pedidoId, Long repartidorId) {
        Incidencia i = incidenciaRepository.save(new Incidencia(null, tipo, nota, entregaId, pedidoId, repartidorId, Instant.now()));
        Map<String, Object> vars = new HashMap<>();
        vars.put("tipo", tipo.name());
        vars.put("nota", nota);
        vars.put("pedidoId", pedidoId == null ? "" : pedidoId);
        vars.put("repartidor", repartidorRepository.findById(repartidorId).map(Repartidor::getNombre).orElse("Repartidor " + repartidorId));
        notificaciones.enviar("INCIDENCIA_REPARTO", "PANEL_ADMIN", "admin", vars);
        return i;
    }

    /* =============== Calificaciones =============== */

    /** CU-09.06 El cliente califica al repartidor (1 a 5 estrellas, una vez por entrega). */
    @Transactional
    public CalificacionRepartidor calificar(Long entregaId, Long clienteId, Integer estrellas) {
        Entrega e = entrega(entregaId);
        if (e.getEstado() != EstadoEntrega.ENTREGADA) {
            throw new ReglaNegocioException("Solo se califican entregas realizadas");
        }
        if (calificacionRepository.existsByEntregaId(entregaId)) {
            throw new ReglaNegocioException("Esta entrega ya fue calificada");
        }
        Repartidor r = obtener(e.getRepartidorId());
        double suma = r.getCalificacionProm() * r.getVotos() + estrellas;
        r.setVotos(r.getVotos() + 1);
        r.setCalificacionProm(Math.round(suma / r.getVotos() * 10) / 10.0);
        repartidorRepository.save(r);
        return calificacionRepository.save(new CalificacionRepartidor(null, entregaId, r.getId(), clienteId, estrellas, Instant.now()));
    }
}
