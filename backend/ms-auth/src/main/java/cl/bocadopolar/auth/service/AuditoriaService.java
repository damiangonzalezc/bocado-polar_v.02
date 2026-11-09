package cl.bocadopolar.auth.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import cl.bocadopolar.auth.model.AuditoriaAcceso;
import cl.bocadopolar.auth.repository.AuditoriaAccesoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaAccesoRepository repository;

    /** Se guarda en su propia transacción para que quede registro aunque la operación falle. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, String email, String accion, boolean exitoso, String ip) {
        repository.save(new AuditoriaAcceso(null, usuarioId, email, accion, exitoso, Instant.now(), ip));
    }

    public List<AuditoriaAcceso> listar(Long usuarioId) {
        return usuarioId == null ? repository.findTop200ByOrderByFechaDesc()
                : repository.findByUsuarioIdOrderByFechaDesc(usuarioId);
    }
}
