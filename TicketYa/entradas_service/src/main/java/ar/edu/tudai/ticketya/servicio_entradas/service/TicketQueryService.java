package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.api.TicketResponse;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class TicketQueryService {
    private final TicketRepository tickets;
    public TicketQueryService(TicketRepository tickets) { this.tickets = tickets; }
    public List<TicketResponse> byOrderId(String orderId) {
        return tickets.findAllByBatch_OrderIdOrderByTicketId(orderId).stream()
                .map(TicketResponse::from).toList();
    }
    public List<TicketResponse> byClientId(String clientId) {
        return tickets.findAllByClientIdOrderByTicketId(clientId).stream()
                .map(TicketResponse::from).toList();
    }
}
