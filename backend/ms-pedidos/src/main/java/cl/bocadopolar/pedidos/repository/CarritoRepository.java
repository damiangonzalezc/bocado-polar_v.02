package cl.bocadopolar.pedidos.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.pedidos.model.Carrito;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByClienteId(Long clienteId);

    Optional<Carrito> findBySesionAnonimaId(String sesionAnonimaId);

    List<Carrito> findByActualizadoEnBefore(Instant limite);
}
