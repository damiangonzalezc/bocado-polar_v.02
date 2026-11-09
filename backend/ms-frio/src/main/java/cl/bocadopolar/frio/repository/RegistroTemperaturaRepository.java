package cl.bocadopolar.frio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.frio.model.OrigenTemp;
import cl.bocadopolar.frio.model.RegistroTemperatura;

@Repository
public interface RegistroTemperaturaRepository extends JpaRepository<RegistroTemperatura, Long> {

    List<RegistroTemperatura> findByOrigenAndReferenciaIdOrderByFechaDesc(OrigenTemp origen, Long referenciaId);

    List<RegistroTemperatura> findByOrigenOrderByFechaDesc(OrigenTemp origen);

    List<RegistroTemperatura> findTop300ByOrderByFechaDesc();
}
