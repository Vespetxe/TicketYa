package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketBatch;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderItemSnapshot;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderSnapshot;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketBatchRepository;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
class TicketBatchWriterTest {
    @Test void createsOneNumberedTicketAndMultipleUnnumberedTickets() {
        TicketBatchRepository batches = mock(TicketBatchRepository.class);
        TicketRepository tickets = mock(TicketRepository.class);
        when(batches.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(tickets.saveAllAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        TicketBatchWriter writer = new TicketBatchWriter(batches, tickets);
        OrderSnapshot order = new OrderSnapshot("order-1", "client-1", "CONFIRMADA", List.of(
                new OrderItemSnapshot("event-1", "platea", "seat-12", 1),
                new OrderItemSnapshot("event-1", "campo", null, 2)));
        List<Ticket> created = writer.create(order);
        assertEquals(3, created.size());
        assertEquals("seat-12", created.getFirst().getSeatId());
        assertNull(created.get(1).getSeatId());
        assertNull(created.get(2).getSeatId());
        assertEquals("order-1", created.get(2).getOrderId());
        assertEquals(3, created.stream().map(Ticket::getTicketId).distinct().count());
        assertEquals(3, created.stream().map(Ticket::getQrPayload).distinct().count());
        verify(tickets).saveAllAndFlush(any());
    }
    @Test void rejectsNumberedSeatLineWithMultipleQuantity() {
        TicketBatchRepository batches = mock(TicketBatchRepository.class);
        TicketBatchWriter writer = new TicketBatchWriter(batches, mock(TicketRepository.class));
        OrderSnapshot order = new OrderSnapshot("order-1", "client-1", "CONFIRMADA",
                List.of(new OrderItemSnapshot("event-1", "platea", "seat-12", 2)));
        assertThrows(RuntimeException.class, () -> writer.create(order));
        verifyNoInteractions(batches);
    }
}
