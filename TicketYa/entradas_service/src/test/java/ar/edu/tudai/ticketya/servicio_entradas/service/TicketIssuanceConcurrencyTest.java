package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderClient;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderItemSnapshot;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderSnapshot;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketBatchRepository;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Import(TicketIssuanceConcurrencyTest.OrderClientConfiguration.class)
class TicketIssuanceConcurrencyTest {
    @Autowired TicketIssuanceService issuance;
    @Autowired TicketBatchRepository batches;
    @Autowired TicketRepository tickets;
    @Autowired StubOrderClient orders;
    @BeforeEach void reset() {
        tickets.deleteAll();
        batches.deleteAll();
        orders.setOrder(new OrderSnapshot("order-race", "client-1", "CONFIRMADA",
                List.of(new OrderItemSnapshot("event-1", "campo", null, 3))));
    }
    @Test void repeatedIssueReturnsThePreviouslyCreatedBatch() {
        TicketIssuanceResult first = issuance.issue("order-race");
        TicketIssuanceResult retry = issuance.issue("order-race");
        assertTrue(first.created());
        assertFalse(retry.created());
        assertEquals(first.tickets(), retry.tickets());
        assertEquals(3, tickets.count());
        assertEquals(1, batches.count());
    }
    @Test void concurrentRequestsForOneOrderCreateOneBatch() throws Exception {
        int callers = 12;
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<TicketIssuanceResult>> futures = java.util.stream.IntStream.range(0, callers)
                    .mapToObj(ignored -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return issuance.issue("order-race");
                    })).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            List<TicketIssuanceResult> results = new java.util.ArrayList<>();
            for (Future<TicketIssuanceResult> future : futures) results.add(future.get(15, TimeUnit.SECONDS));
            assertEquals(1, results.stream().filter(TicketIssuanceResult::created).count());
            assertEquals(3, tickets.count());
            assertEquals(1, batches.count());
            List<String> returnedIds = results.getFirst().tickets().stream().map(t -> t.ticketId()).toList();
            assertTrue(results.stream().allMatch(r ->
                    r.tickets().stream().map(t -> t.ticketId()).toList().equals(returnedIds)));
        } finally {
            executor.shutdownNow();
        }
    }
    @TestConfiguration
    static class OrderClientConfiguration {
        @Bean @Primary StubOrderClient stubOrderClient() { return new StubOrderClient(); }
    }
    static class StubOrderClient implements OrderClient {
        private volatile OrderSnapshot order;
        void setOrder(OrderSnapshot order) { this.order = order; }
        @Override public OrderSnapshot getOrder(String orderId) {
            if (order == null || !order.orderId().equals(orderId)) throw new IllegalArgumentException("unexpected orderId");
            return order;
        }
    }
}
