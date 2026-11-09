package cl.bocadopolar.notificaciones.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.notificaciones.model.Canal;
import cl.bocadopolar.notificaciones.model.EstadoEnvio;
import cl.bocadopolar.notificaciones.model.Notificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Optional<Notificacion> findByEventoId(String eventoId);

    List<Notificacion> findByEstado(EstadoEnvio estado);

    List<Notificacion> findByCanalOrderByCreadaEnDesc(Canal canal);
}
