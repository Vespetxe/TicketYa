package ar.edu.tudai.ticketya.servicio_entradas.domain;
import ar.edu.tudai.ticketya.servicio_entradas.api.TicketResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class TicketTest {
    @Test void numberedTicketKeepsItsSeatReference() {
        Ticket ticket = new Ticket("ticket-1", "qr-1", new TicketBatch("order-1"),
                "event-1", "client-1", "sector-1", "seat-1");
        TicketResponse response = TicketResponse.from(ticket);
        assertEquals("order-1", response.orderId());
        assertEquals("event-1", response.eventId());
        assertEquals("client-1", response.clientId());
        assertEquals("sector-1", response.sectorId());
        assertEquals("seat-1", response.seatId());
        assertEquals("EMITIDA", response.status());
    }
    @Test void unnumberedTicketDoesNotRequireASeatId() {
        Ticket ticket = new Ticket("ticket-2", "qr-2", new TicketBatch("order-2"),
                "event-1", "client-1", "sector-campo", null);
        TicketResponse response = TicketResponse.from(ticket);
        assertEquals("sector-campo", response.sectorId());
        assertNull(response.seatId());
    }
}
