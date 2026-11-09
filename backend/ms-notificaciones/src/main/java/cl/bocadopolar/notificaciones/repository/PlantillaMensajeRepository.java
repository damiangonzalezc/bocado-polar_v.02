package cl.bocadopolar.notificaciones.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.notificaciones.model.MotivoNotificacion;
import cl.bocadopolar.notificaciones.model.PlantillaMensaje;

@Repository
public interface PlantillaMensajeRepository extends JpaRepository<PlantillaMensaje, Long> {

    Optional<PlantillaMensaje> findByMotivo(MotivoNotificacion motivo);
}
