package cl.bocadopolar.configuracion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.configuracion.model.PasarelaPago;

@Repository
public interface PasarelaPagoRepository extends JpaRepository<PasarelaPago, Long> {

    Optional<PasarelaPago> findByNombreIgnoreCase(String nombre);
}
