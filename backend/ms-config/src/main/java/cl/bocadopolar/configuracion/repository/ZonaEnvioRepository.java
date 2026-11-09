package cl.bocadopolar.configuracion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.configuracion.model.ZonaEnvio;

@Repository
public interface ZonaEnvioRepository extends JpaRepository<ZonaEnvio, Long> {

    Optional<ZonaEnvio> findByComunaIgnoreCase(String comuna);

    List<ZonaEnvio> findByActivaTrueOrderByComunaAsc();

    boolean existsByComunaIgnoreCase(String comuna);
}
