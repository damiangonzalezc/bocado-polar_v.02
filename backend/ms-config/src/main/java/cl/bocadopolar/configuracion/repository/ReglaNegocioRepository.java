package cl.bocadopolar.configuracion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.configuracion.model.ReglaNegocio;

@Repository
public interface ReglaNegocioRepository extends JpaRepository<ReglaNegocio, String> {
}
