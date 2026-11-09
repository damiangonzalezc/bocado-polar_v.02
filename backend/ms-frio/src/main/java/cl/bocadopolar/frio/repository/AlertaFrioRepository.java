package cl.bocadopolar.frio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.frio.model.AlertaFrio;
import cl.bocadopolar.frio.model.OrigenTemp;

@Repository
public interface AlertaFrioRepository extends JpaRepository<AlertaFrio, Long> {

    List<AlertaFrio> findAllByOrderByFechaDesc();

    List<AlertaFrio> findByAtendidaOrderByFechaDesc(Boolean atendida);

    List<AlertaFrio> findByOrigenAndReferenciaIdAndAtendidaFalse(OrigenTemp origen, Long referenciaId);
}
