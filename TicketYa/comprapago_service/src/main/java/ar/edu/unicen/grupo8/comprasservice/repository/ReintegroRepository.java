
package ar.edu.unicen.grupo8.comprasservice.repository;

import ar.edu.unicen.grupo8.comprasservice.entity.Reintegro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReintegroRepository extends JpaRepository<Reintegro, Long> {

    List<Reintegro> findByPagoId(Long pagoId);

    List<Reintegro> findByDevolucionId(Long devolucionId);

    Optional<Reintegro> findByClaveIdempotencia(String claveIdempotencia);

    boolean existsByClaveIdempotencia(String claveIdempotencia);
}
