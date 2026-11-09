package cl.bocadopolar.catalogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.catalogo.model.Valoracion;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {

    Optional<Valoracion> findByProductoIdAndClienteId(Long productoId, Long clienteId);

    List<Valoracion> findByProductoId(Long productoId);
}
