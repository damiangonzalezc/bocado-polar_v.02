package cl.bocadopolar.frio.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.frio.model.CuentaRegresivaRuta;

@Repository
public interface CuentaRegresivaRutaRepository extends JpaRepository<CuentaRegresivaRuta, Long> {

    Optional<CuentaRegresivaRuta> findByTurnoId(Long turnoId);
}
