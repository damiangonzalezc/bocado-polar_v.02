package cl.bocadopolar.pagos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.pagos.model.CobroContraEntrega;

@Repository
public interface CobroContraEntregaRepository extends JpaRepository<CobroContraEntrega, Long> {

    Optional<CobroContraEntrega> findByPagoId(Long pagoId);

    List<CobroContraEntrega> findByCobradoPorIdAndCobradoTrue(Long cobradoPorId);
}
