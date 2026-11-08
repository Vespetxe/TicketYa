package example.microservicio_inventario_reservas.repositories;

import example.microservicio_inventario_reservas.entities.InventarioSector;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("InventarioSectorRepository")
public interface InventarioSectorRepository extends JpaRepository<InventarioSector, Long> {

    Optional<InventarioSector> findByIdEventoAndIdSector(Long idEvento, Long idSector);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i FROM InventarioSector i
        WHERE i.idEvento = :idEvento AND i.idSector = :idSector
    """)
    Optional<InventarioSector> findByIdEventoAndIdSectorWithLock(@Param("idEvento") Long idEvento, @Param("idSector") Long idSector);

    @Modifying
    @Query("""
        UPDATE InventarioSector i SET i.disponibles = i.disponibles + :cantidad
        WHERE i.idEvento = :idEvento AND i.idSector = :idSector AND i.esNumerado = false
    """)
    int incrementarDisponibilidad(@Param("idEvento") Long idEvento, @Param("idSector") Long idSector, @Param("cantidad") Integer cantidad);

    @Modifying
    @Query("""
        UPDATE InventarioSector i SET i.estado = example.microservicio_inventario_reservas.enums.EstadoInventario.CANCELADO, i.disponibles = 0
        WHERE i.idEvento = :idEvento
    """)
    int cancelarInventarioPorEvento(@Param("idEvento") Long idEvento);
}
