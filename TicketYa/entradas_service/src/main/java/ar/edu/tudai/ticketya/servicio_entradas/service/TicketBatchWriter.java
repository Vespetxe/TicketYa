package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.domain.InvalidOrderException;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketBatch;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderItemSnapshot;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderSnapshot;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketBatchRepository;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
@Service
public class TicketBatchWriter {
    private final TicketBatchRepository batches;
    private final TicketRepository tickets;
    public TicketBatchWriter(TicketBatchRepository batches, TicketRepository tickets) {
        this.batches = batches;
        this.tickets = tickets;
    }
    @Transactional
    public List<Ticket> create(OrderSnapshot order) {
        validate(order);
        TicketBatch batch = batches.saveAndFlush(new TicketBatch(order.orderId()));
        List<Ticket> created = new ArrayList<>();
        Set<String> numberedSeats = new HashSet<>();
        for (OrderItemSnapshot item : order.items()) {
            String seatId = StringUtils.hasText(item.seatId()) ? item.seatId().trim() : null;
            if (seatId != null && !numberedSeats.add(item.eventId() + "|" + item.sectorId() + "|" + seatId)) {
                throw new InvalidOrderException("The order contains the same numbered seat more than once");
            }
            for (int i = 0; i < item.quantity(); i++) {
                created.add(new Ticket(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                        batch, item.eventId(), order.clientId(), item.sectorId(), seatId));
            }
        }
        return tickets.saveAllAndFlush(created);
    }
    private void validate(OrderSnapshot order) {
        if (order == null || !StringUtils.hasText(order.orderId()) || !StringUtils.hasText(order.clientId())
                || order.items() == null || order.items().isEmpty()) {
            throw new InvalidOrderException("The order must contain an id, client, and at least one item");
        }
        for (OrderItemSnapshot item : order.items()) {
            if (item == null || !StringUtils.hasText(item.eventId()) || !StringUtils.hasText(item.sectorId())
                    || item.quantity() < 1) {
                throw new InvalidOrderException("Every order item must have eventId, sectorId, and a positive quantity");
            }
            if (StringUtils.hasText(item.seatId()) && item.quantity() != 1) {
                throw new InvalidOrderException("A numbered-seat order item must have quantity 1");
            }
        }
    }
}
