
package ar.edu.unicen.grupo8.comprasservice.repository;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoPago;

import ar.edu.unicen.grupo8.comprasservice.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByCompraId(Long compraId);

    Optional<Pago> findByClaveIdempotencia(String claveIdempotencia);

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    boolean existsByCompraIdAndEstado(
            Long compraId,
            EstadoPago estado
    );
}
