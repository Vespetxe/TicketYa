package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.api.TicketResponse;
import ar.edu.tudai.ticketya.servicio_entradas.domain.InvalidTicketStatusException;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketNotFoundException;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketStatus;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class TicketStatusService {
    private final TicketRepository tickets;
    public TicketStatusService(TicketRepository tickets) { this.tickets = tickets; }
    @Transactional
    public TicketResponse updateOne(String ticketId, String orderId, TicketStatus requested) {
        validate(requested);
        Ticket ticket = tickets.findByTicketIdAndBatch_OrderId(ticketId, orderId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        transition(ticket, requested);
        return TicketResponse.from(tickets.saveAndFlush(ticket));
    }
    @Transactional
    public List<TicketResponse> updateForOrderEvent(String orderId, String eventId, TicketStatus requested) {
        validate(requested);
        List<Ticket> affected = tickets.findAllByBatch_OrderIdAndEventIdOrderByTicketId(orderId, eventId);
        affected.forEach(ticket -> transition(ticket, requested));
        return tickets.saveAllAndFlush(affected).stream().map(TicketResponse::from).toList();
    }
    private void validate(TicketStatus requested) {
        if (requested != TicketStatus.ANULADA && requested != TicketStatus.REINTEGRADA) {
            throw new InvalidTicketStatusException();
        }
    }
    private void transition(Ticket ticket, TicketStatus requested) {
        if (ticket.getStatus() == TicketStatus.REINTEGRADA) return;
        if (ticket.getStatus() == TicketStatus.ANULADA && requested == TicketStatus.ANULADA) return;
        ticket.setStatus(requested);
    }
}
