package cl.bocadopolar.reparto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reparto.model.Turno;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    java.util.List<Turno> findByRepartidorIdAndFinIsNull(Long repartidorId);
}
