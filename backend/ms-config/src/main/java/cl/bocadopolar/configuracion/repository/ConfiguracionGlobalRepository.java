package cl.bocadopolar.configuracion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.configuracion.model.ConfiguracionGlobal;

@Repository
public interface ConfiguracionGlobalRepository extends JpaRepository<ConfiguracionGlobal, Long> {

    Optional<ConfiguracionGlobal> findFirstByOrderByIdAsc();
}
