package ar.edu.tudai.ticketya.servicio_entradas.repository;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketBatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertThrows;
@SpringBootTest
@Transactional
class TicketPersistenceTest {
    @Autowired TicketBatchRepository batches;
    @Autowired TicketRepository tickets;
    @Autowired JdbcTemplate jdbc;
    @Test void orderHasOnlyOneIssuanceBatch() {
        batches.saveAndFlush(new TicketBatch("order-1"));
        assertThrows(DataIntegrityViolationException.class,
                () -> batches.saveAndFlush(new TicketBatch("order-1")));
    }
    @Test void qrPayloadIsUniqueAcrossTickets() {
        TicketBatch batch = batches.saveAndFlush(new TicketBatch("order-2"));
        tickets.saveAndFlush(new Ticket("ticket-1", "same-qr", batch,
                "event-1", "client-1", "sector-1", "seat-1"));
        assertThrows(DataIntegrityViolationException.class,
                () -> tickets.saveAndFlush(new Ticket("ticket-2", "same-qr", batch,
                        "event-1", "client-1", "sector-1", null)));
    }
    @Test void ticketIdIsThePrimaryUniqueKey() {
        TicketBatch batch = batches.saveAndFlush(new TicketBatch("order-3"));
        tickets.saveAndFlush(new Ticket("ticket-1", "qr-1", batch,
                "event-1", "client-1", "sector-1", "seat-1"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into tickets (ticket_id, qr_payload, batch_id, event_id, client_id, sector_id, status) values (?, ?, ?, ?, ?, ?, ?)",
                "ticket-1", "qr-2", batch.getId(), "event-1", "client-1", "sector-1", "EMITIDA"));
    }
}
