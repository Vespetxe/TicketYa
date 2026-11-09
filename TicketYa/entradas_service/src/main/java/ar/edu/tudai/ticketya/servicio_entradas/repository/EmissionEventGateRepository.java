package ar.edu.tudai.ticketya.servicio_entradas.repository;
import ar.edu.tudai.ticketya.servicio_entradas.domain.EmissionEventGate;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface EmissionEventGateRepository extends JpaRepository<EmissionEventGate,String> {
    @Modifying @Query(value="INSERT INTO emission_event_gate(event_id,cancelled) VALUES (:id,false) ON DUPLICATE KEY UPDATE event_id=event_id",nativeQuery=true)
    void ensure(@Param("id") String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select g from EmissionEventGate g where g.eventId=:id")
    Optional<EmissionEventGate> lock(@Param("id") String id);
}
