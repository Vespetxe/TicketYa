package example.microservicio_inventario_reservas.repositories;
import example.microservicio_inventario_reservas.entities.ControlEvento;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ControlEventoRepository extends JpaRepository<ControlEvento,Long> {
    @Modifying @Query(value="INSERT INTO control_evento (id_evento,estado) VALUES (:id,'PREPARADO') ON DUPLICATE KEY UPDATE id_evento=id_evento",nativeQuery=true)
    void asegurar(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from ControlEvento c where c.id=:id")
    Optional<ControlEvento> bloquear(@Param("id") Long id);
}
