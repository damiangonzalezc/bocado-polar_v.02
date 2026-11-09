package cl.bocadopolar.auth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.auth.model.PermisoRol;
import cl.bocadopolar.auth.model.Rol;

@Repository
public interface PermisoRolRepository extends JpaRepository<PermisoRol, Long> {

    List<PermisoRol> findByRol(Rol rol);

    boolean existsByRolAndSeccionAndConcedidoTrue(Rol rol, String seccion);

    void deleteByRol(Rol rol);
}
