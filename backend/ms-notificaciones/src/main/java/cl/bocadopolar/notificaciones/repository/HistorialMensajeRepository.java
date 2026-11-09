package cl.bocadopolar.notificaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.notificaciones.model.HistorialMensaje;

@Repository
public interface HistorialMensajeRepository extends JpaRepository<HistorialMensaje, Long> {

    List<HistorialMensaje> findByNotificacionIdOrderByFechaAsc(Long notificacionId);
}
