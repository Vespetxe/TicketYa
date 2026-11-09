package ar.edu.tudai.ticketya.servicio_entradas.repository;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TicketBatchRepository extends JpaRepository<TicketBatch, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from TicketBatch b where b.orderId=:id")
    Optional<TicketBatch> lockByOrderId(@org.springframework.data.repository.query.Param("id") String id);
    Optional<TicketBatch> findByOrderId(String orderId);
}
