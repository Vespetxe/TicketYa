package example.microservicio_inventario_reservas.repositories;

import example.microservicio_inventario_reservas.entities.Asiento;
import example.microservicio_inventario_reservas.enums.EstadoAsiento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("AsientoRepository")
public interface AsientoRepository extends JpaRepository<Asiento, Long> {

    List<Asiento> findByInventarioSectorIdInventarioSector(Long idInventarioSector);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM Asiento a
        WHERE a.idAsiento IN :idsAsientos 
        AND a.inventarioSector.idInventarioSector = :idInventario
    """)
    List<Asiento> findByIdsWithLock(@Param("idsAsientos") List<Long> idsAsientos, @Param("idInventario") Long idInventario);

    @Modifying
    @Query("""
        UPDATE Asiento a SET a.estado = :nuevoEstado
        WHERE a.idAsiento IN :idsAsientos
    """)
    int actualizarEstadoAsientos(@Param("idsAsientos") List<Long> idsAsientos, @Param("nuevoEstado") EstadoAsiento nuevoEstado);

    @Modifying
    @Query("""
        UPDATE Asiento a SET a.estado = example.microservicio_inventario_reservas.enums.EstadoAsiento.CANCELADO
        WHERE a.inventarioSector.idEvento = :idEvento
    """)
    int cancelarAsientosPorEvento(@Param("idEvento") Long idEvento);
}
