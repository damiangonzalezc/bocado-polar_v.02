package cl.bocadopolar.reparto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reparto.model.CalificacionRepartidor;

@Repository
public interface CalificacionRepartidorRepository extends JpaRepository<CalificacionRepartidor, Long> {

    boolean existsByEntregaId(Long entregaId);
}
