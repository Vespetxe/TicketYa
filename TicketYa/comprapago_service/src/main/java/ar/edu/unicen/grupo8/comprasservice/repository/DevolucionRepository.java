
package ar.edu.unicen.grupo8.comprasservice.repository;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion;

import ar.edu.unicen.grupo8.comprasservice.entity.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT d FROM Devolucion d WHERE d.id = :id")
    java.util.Optional<Devolucion> buscarConBloqueo(@org.springframework.data.repository.query.Param("id") Long id);

    List<Devolucion> findByCompraId(Long compraId);

    List<Devolucion> findByEstado(EstadoDevolucion estado);

    List<Devolucion> findByCompraEventoId(Long eventoId);
    @org.springframework.data.jpa.repository.Query("select d.compra.id from Devolucion d where d.id = :id")
    java.util.Optional<Long> buscarCompraId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select d.id from Devolucion d where d.compra.id = :compraId order by d.id")
    java.util.List<Long> buscarIdsPorCompra(@org.springframework.data.repository.query.Param("compraId") Long compraId);
}
