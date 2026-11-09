package cl.bocadopolar.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.auth.model.AuditoriaAcceso;

@Repository
public interface AuditoriaAccesoRepository extends JpaRepository<AuditoriaAcceso, Long> {

    List<AuditoriaAcceso> findTop200ByOrderByFechaDesc();

    List<AuditoriaAcceso> findByUsuarioIdOrderByFechaDesc(Long usuarioId);
}
