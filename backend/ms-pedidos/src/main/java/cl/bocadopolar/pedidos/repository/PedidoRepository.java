package cl.bocadopolar.pedidos.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.bocadopolar.pedidos.model.EstadoPedido;
import cl.bocadopolar.pedidos.model.Pedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByCodigoIgnoreCase(String codigo);

    List<Pedido> findByClienteIdOrderByCreadoEnDesc(Long clienteId);

    List<Pedido> findByEstadoAndCreadoEnBefore(EstadoPedido estado, Instant limite);
}
