package cl.bocadopolar.notificaciones.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.notificaciones.dto.NotificacionDtos.EventoRequest;
import cl.bocadopolar.notificaciones.dto.NotificacionDtos.PlantillaRequest;
import cl.bocadopolar.notificaciones.exception.RecursoNoEncontradoException;
import cl.bocadopolar.notificaciones.exception.ReglaNegocioException;
import cl.bocadopolar.notificaciones.model.Canal;
import cl.bocadopolar.notificaciones.model.EstadoEnvio;
import cl.bocadopolar.notificaciones.model.HistorialMensaje;
import cl.bocadopolar.notificaciones.model.MotivoNotificacion;
import cl.bocadopolar.notificaciones.model.Notificacion;
import cl.bocadopolar.notificaciones.model.PlantillaMensaje;
import cl.bocadopolar.notificaciones.repository.HistorialMensajeRepository;
import cl.bocadopolar.notificaciones.repository.NotificacionRepository;
import cl.bocadopolar.notificaciones.repository.PlantillaMensajeRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificacionService {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[a-zA-Z]{2,}$");

    private final NotificacionRepository repository;
    private final PlantillaMensajeRepository plantillaRepository;
    private final HistorialMensajeRepository historialRepository;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean correoHabilitado;
    private final String remitente;

    public NotificacionService(NotificacionRepository repository, PlantillaMensajeRepository plantillaRepository,
            HistorialMensajeRepository historialRepository, ObjectProvider<JavaMailSender> mailSender,
            @Value("${bocado.correo.habilitado:false}") boolean correoHabilitado,
            @Value("${bocado.correo.remitente:no-responder@bocadopolar.cl}") String remitente) {
        this.repository = repository;
        this.plantillaRepository = plantillaRepository;
        this.historialRepository = historialRepository;
        this.mailSender = mailSender;
        this.correoHabilitado = correoHabilitado;
        this.remitente = remitente;
    }

    /* =============== Eventos =============== */

    /** CU-13 Recibe un evento, arma el mensaje con su plantilla y lo envía. Idempotente por eventoId. */
    @Transactional
    public Notificacion procesarEvento(EventoRequest req) {
        if (req.eventoId() != null && !req.eventoId().isBlank()) {
            var previa = repository.findByEventoId(req.eventoId());
            if (previa.isPresent()) {
                return previa.get();
            }
        }
        Map<String, Object> vars = req.variables() == null ? Map.of() : req.variables();
        PlantillaMensaje plantilla = plantillaRepository.findByMotivo(req.motivo())
                .filter(PlantillaMensaje::getActiva).orElse(null);
        Notificacion n = new Notificacion();
        n.setEventoId(req.eventoId() == null || req.eventoId().isBlank() ? null : req.eventoId());
        n.setMotivo(req.motivo());
        n.setCanal(req.canal() == null ? Canal.CORREO : req.canal());
        n.setDestinatario(req.destinatario().trim());
        n.setAsunto(recortar(plantilla == null ? "BOCADO Polar · " + req.motivo() : renderizar(plantilla.getAsunto(), vars), 150));
        n.setMensaje(plantilla == null ? vars.toString() : renderizar(plantilla.getCuerpo(), vars));
        n.setCreadaEn(Instant.now());
        n = repository.save(n);
        historial(n, "Recibida desde evento " + req.motivo());
        enviar(n);
        return n;
    }

    /** Intenta el envío según el canal. En desarrollo los correos se simulan (quedan en el log y en la BD). */
    void enviar(Notificacion n) {
        n.setIntentos(n.getIntentos() + 1);
        try {
            switch (n.getCanal()) {
                case CORREO -> {
                    if (!CORREO.matcher(n.getDestinatario()).matches()) {
                        throw new IllegalArgumentException("Destinatario sin correo válido: " + n.getDestinatario());
                    }
                    JavaMailSender sender = mailSender.getIfAvailable();
                    if (correoHabilitado && sender != null) {
                        SimpleMailMessage m = new SimpleMailMessage();
                        m.setFrom(remitente);
                        m.setTo(n.getDestinatario());
                        m.setSubject(n.getAsunto());
                        m.setText(n.getMensaje());
                        sender.send(m);
                        historial(n, "Correo enviado por SMTP");
                    } else {
                        log.info("[CORREO SIMULADO] Para: {} | Asunto: {} | {}", n.getDestinatario(), n.getAsunto(), n.getMensaje());
                        historial(n, "Correo simulado (bocado.correo.habilitado=false)");
                    }
                }
                case PANEL_ADMIN -> historial(n, "Publicada en el panel de administración");
                case SMS -> {
                    log.info("[SMS SIMULADO] Para: {} | {}", n.getDestinatario(), n.getMensaje());
                    historial(n, "SMS simulado");
                }
                default -> throw new IllegalStateException("Canal no soportado");
            }
            n.setEstado(EstadoEnvio.ENVIADA);
            n.setEnviadaEn(Instant.now());
            n.setUltimoError(null);
            n.setProximoReintento(null);
        } catch (Exception e) {
            n.setEstado(EstadoEnvio.FALLIDA);
            n.setUltimoError(recortar(e.getMessage(), 300));
            n.setProximoReintento(n.getIntentos() < n.getMaxIntentos()
                    ? Instant.now().plus(5L * n.getIntentos(), ChronoUnit.MINUTES) : null);
            historial(n, recortar("Falló el intento " + n.getIntentos() + ": " + e.getMessage(), 300));
        }
        repository.save(n);
    }

    @Transactional
    public Notificacion reenviar(Long id) {
        Notificacion n = obtener(id);
        if (n.getEstado() == EstadoEnvio.ENVIADA) {
            throw new ReglaNegocioException("La notificación ya fue enviada");
        }
        enviar(n);
        return n;
    }

    @Transactional
    public int reintentarFallidas() {
        Instant ahora = Instant.now();
        List<Notificacion> pendientes = repository.findByEstado(EstadoEnvio.FALLIDA).stream()
                .filter(Notificacion::puedeReintentar)
                .filter(n -> n.getProximoReintento() == null || !n.getProximoReintento().isAfter(ahora))
                .toList();
        pendientes.forEach(this::enviar);
        return pendientes.size();
    }

    /* =============== Consultas =============== */

    public List<Notificacion> listar(EstadoEnvio estado, MotivoNotificacion motivo, Canal canal, String destinatario) {
        return repository.findAll().stream()
                .filter(n -> estado == null || n.getEstado() == estado)
                .filter(n -> motivo == null || n.getMotivo() == motivo)
                .filter(n -> canal == null || n.getCanal() == canal)
                .filter(n -> destinatario == null || n.getDestinatario().equalsIgnoreCase(destinatario.trim()))
                .sorted(Comparator.comparing(Notificacion::getCreadaEn).reversed())
                .toList();
    }

    public Notificacion obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la notificación " + id));
    }

    public List<HistorialMensaje> historialDe(Long id) {
        obtener(id);
        return historialRepository.findByNotificacionIdOrderByFechaAsc(id);
    }

    /** Avisos del panel admin (alertas de frío, stock e incidencias). */
    public List<Notificacion> panel(boolean soloNoLeidas) {
        return repository.findByCanalOrderByCreadaEnDesc(Canal.PANEL_ADMIN).stream()
                .filter(n -> !soloNoLeidas || !Boolean.TRUE.equals(n.getLeida()))
                .toList();
    }

    @Transactional
    public Notificacion marcarLeida(Long id) {
        Notificacion n = obtener(id);
        n.setLeida(true);
        return repository.save(n);
    }

    /* =============== Plantillas =============== */

    public List<PlantillaMensaje> plantillas() {
        return plantillaRepository.findAll();
    }

    public PlantillaMensaje plantilla(Long id) {
        return plantillaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la plantilla " + id));
    }

    @Transactional
    public PlantillaMensaje crearPlantilla(PlantillaRequest req) {
        if (plantillaRepository.findByMotivo(req.motivo()).isPresent()) {
            throw new ReglaNegocioException("Ya hay una plantilla para " + req.motivo() + "; edítala");
        }
        return plantillaRepository.save(new PlantillaMensaje(null, req.motivo(), req.asunto(), req.cuerpo(),
                req.activa() == null || req.activa()));
    }

    @Transactional
    public PlantillaMensaje actualizarPlantilla(Long id, PlantillaRequest req) {
        PlantillaMensaje p = plantilla(id);
        plantillaRepository.findByMotivo(req.motivo()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw new ReglaNegocioException("Ya hay otra plantilla para " + req.motivo());
        });
        p.setMotivo(req.motivo());
        p.setAsunto(req.asunto());
        p.setCuerpo(req.cuerpo());
        if (req.activa() != null) {
            p.setActiva(req.activa());
        }
        return plantillaRepository.save(p);
    }

    @Transactional
    public void eliminarPlantilla(Long id) {
        plantillaRepository.delete(plantilla(id));
    }

    /* =============== utilidades =============== */

    static String renderizar(String texto, Map<String, Object> vars) {
        Matcher m = VARIABLE.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Object v = vars.get(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : String.valueOf(v)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void historial(Notificacion n, String detalle) {
        historialRepository.save(new HistorialMensaje(null, n.getId(), detalle, Instant.now()));
    }

    private static String recortar(String t, int max) {
        if (t == null) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max - 3) + "...";
    }
}
