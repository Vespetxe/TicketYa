
package ar.edu.unicen.grupo8.comprasservice.repository;

import ar.edu.unicen.grupo8.comprasservice.entity.DetalleDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleDevolucionRepository
        extends JpaRepository<DetalleDevolucion, Long> {

    List<DetalleDevolucion> findByDevolucionId(Long devolucionId);

    List<DetalleDevolucion> findByDetalleCompraId(Long detalleCompraId);

    boolean existsByEntradaId(Long entradaId);

    boolean existsByEntradaIdAndDevolucionEstadoNot(Long entradaId,
            ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion estado);
}
