package cl.bocadopolar.configuracion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.configuracion.model.EmpresaLogistica;

@Repository
public interface EmpresaLogisticaRepository extends JpaRepository<EmpresaLogistica, Long> {
}
