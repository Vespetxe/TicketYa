package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.api.TicketResponse;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderClient;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderSnapshot;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketBatchRepository;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
@Service
public class TicketIssuanceService {
    private final OrderClient orderClient;
    private final TicketBatchRepository batches;
    private final TicketRepository tickets;
    private final TicketBatchWriter writer;
    public TicketIssuanceService(OrderClient orderClient, TicketBatchRepository batches,
            TicketRepository tickets, TicketBatchWriter writer) {
        this.orderClient = orderClient;
        this.batches = batches;
        this.tickets = tickets;
        this.writer = writer;
    }
    public TicketIssuanceResult issue(String orderId) {
        List<Ticket> existing = existingTickets(orderId);
        if (existing != null) return result(existing, false);
        OrderSnapshot order = orderClient.getOrder(orderId);
        try {
            return result(writer.create(order), true);
        } catch (DataIntegrityViolationException duplicateOrder) {
            List<Ticket> winner = existingTickets(orderId);
            if (winner == null) throw duplicateOrder;
            return result(winner, false);
        }
    }
    private List<Ticket> existingTickets(String orderId) {
        return batches.findByOrderId(orderId)
                .map(batch -> tickets.findAllByBatch_OrderIdOrderByTicketId(orderId))
                .orElse(null);
    }
    private TicketIssuanceResult result(List<Ticket> values, boolean created) {
        List<TicketResponse> response = values.stream().map(TicketResponse::from)
                .sorted(Comparator.comparing(TicketResponse::ticketId)).toList();
        return new TicketIssuanceResult(response, created);
    }
}
