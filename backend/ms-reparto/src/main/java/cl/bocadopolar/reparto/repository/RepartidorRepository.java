package cl.bocadopolar.reparto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.reparto.model.Repartidor;

@Repository
public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    java.util.Optional<Repartidor> findByEmailIgnoreCase(String email);
}
