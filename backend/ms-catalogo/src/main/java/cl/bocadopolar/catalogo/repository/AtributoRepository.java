package cl.bocadopolar.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.catalogo.model.Atributo;

@Repository
public interface AtributoRepository extends JpaRepository<Atributo, Long> {

    Optional<Atributo> findByNombreIgnoreCase(String nombre);
}
