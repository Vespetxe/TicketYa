package ar.edu.tudai.ticketya.servicio_entradas.repository;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TicketBatchRepository extends JpaRepository<TicketBatch, Long> {
    Optional<TicketBatch> findByOrderId(String orderId);
}
