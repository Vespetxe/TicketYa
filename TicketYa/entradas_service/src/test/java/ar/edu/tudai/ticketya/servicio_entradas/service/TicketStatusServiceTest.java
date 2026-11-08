package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.domain.*;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class TicketStatusServiceTest {
    private final TicketRepository repository = mock(TicketRepository.class);
    private final TicketStatusService service = new TicketStatusService(repository);
    private Ticket ticket(TicketStatus status) {
        Ticket ticket = new Ticket("ticket-1", "qr-1", new TicketBatch("order-1"),
                "event-1", "client-1", "campo", null);
        ticket.setStatus(status);
        return ticket;
    }
    @Test void annulmentIsIdempotent() {
        Ticket value = ticket(TicketStatus.EMITIDA);
        when(repository.findByTicketIdAndBatch_OrderId("ticket-1", "order-1"))
                .thenReturn(Optional.of(value));
        when(repository.saveAndFlush(value)).thenReturn(value);
        assertEquals("ANULADA", service.updateOne("ticket-1", "order-1", TicketStatus.ANULADA).status());
        assertEquals("ANULADA", service.updateOne("ticket-1", "order-1", TicketStatus.ANULADA).status());
        verify(repository, times(2)).saveAndFlush(value);
    }
    @Test void reintegratedTicketCannotBeDowngraded() {
        Ticket value = ticket(TicketStatus.REINTEGRADA);
        when(repository.findByTicketIdAndBatch_OrderId("ticket-1", "order-1"))
                .thenReturn(Optional.of(value));
        when(repository.saveAndFlush(value)).thenReturn(value);
        assertEquals("REINTEGRADA", service.updateOne("ticket-1", "order-1", TicketStatus.ANULADA).status());
    }
    @Test void unknownTicketOrOrderIsRejected() {
        when(repository.findByTicketIdAndBatch_OrderId("missing", "order-1")).thenReturn(Optional.empty());
        assertThrows(TicketNotFoundException.class,
                () -> service.updateOne("missing", "order-1", TicketStatus.ANULADA));
    }
    @Test void emittedIsNotAnInvalidationStatus() {
        assertThrows(InvalidTicketStatusException.class,
                () -> service.updateOne("ticket-1", "order-1", TicketStatus.EMITIDA));
        verifyNoInteractions(repository);
    }
}
