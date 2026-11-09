package cl.bocadopolar.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.catalogo.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    long countByCategoriaId(Long categoriaId);
}
