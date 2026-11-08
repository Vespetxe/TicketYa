package ar.edu.tudai.ticketya.servicio_entradas.repository;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface TicketRepository extends JpaRepository<Ticket, String> {
    @EntityGraph(attributePaths = "batch")
    List<Ticket> findAllByBatch_OrderIdOrderByTicketId(String orderId);
    @EntityGraph(attributePaths = "batch")
    List<Ticket> findAllByClientIdOrderByTicketId(String clientId);
    @EntityGraph(attributePaths = "batch")
    Optional<Ticket> findByTicketIdAndBatch_OrderId(String ticketId, String orderId);
    @EntityGraph(attributePaths = "batch")
    List<Ticket> findAllByBatch_OrderIdAndEventIdOrderByTicketId(String orderId, String eventId);
}
