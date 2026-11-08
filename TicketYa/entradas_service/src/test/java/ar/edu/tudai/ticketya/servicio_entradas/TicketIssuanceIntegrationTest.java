package ar.edu.tudai.ticketya.servicio_entradas;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketBatchRepository;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketIssuanceIntegrationTest {
    private static HttpServer compras;
    private static final String CONFIRMED_ORDER = """
            {"orderId":"confirmed","clientId":"client-1","status":"CONFIRMADA","items":[
              {"eventId":"event-1","sectorId":"platea","seatId":"seat-12","quantity":1},
              {"eventId":"event-1","sectorId":"campo","seatId":null,"quantity":2}]}
            """;
    @Value("${local.server.port}") int port;
    @Autowired TicketRepository tickets;
    @Autowired TicketBatchRepository batches;
    @DynamicPropertySource
    static void configureOrdersUrl(DynamicPropertyRegistry registry) {
        try {
            compras = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            compras.createContext("/internal/orders/", exchange -> {
                String orderId = exchange.getRequestURI().getPath().substring("/internal/orders/".length());
                int status = orderId.equals("missing") ? 404 : 200;
                String body = orderId.equals("confirmed") ? CONFIRMED_ORDER
                        : """
                          {"orderId":"pending","clientId":"client-1","status":"PENDIENTE",
                           "items":[{"eventId":"event-1","sectorId":"campo","seatId":null,"quantity":1}]}
                          """;
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            compras.start();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        registry.add("ticketya.compras.base-url",
                () -> "http://localhost:" + compras.getAddress().getPort());
    }
    @AfterAll static void stopCompras() {
        if (compras != null) compras.stop(0);
    }
    @BeforeEach void cleanDatabase() {
        tickets.deleteAll();
        batches.deleteAll();
    }
    @Test void confirmedOrderEmitsOnceAndRetryReturnsSameTickets() throws Exception {
        HttpResponse<String> created = issue("confirmed");
        assertEquals(201, created.statusCode());
        assertTrue(created.body().contains("\"created\":true"));
        assertTrue(created.body().contains("\"seatId\":\"seat-12\""));
        assertTrue(created.body().contains("\"sectorId\":\"campo\""));
        assertEquals(3, tickets.count());
        HttpResponse<String> retry = issue("confirmed");
        assertEquals(200, retry.statusCode());
        assertTrue(retry.body().contains("\"created\":false"));
        assertEquals(3, tickets.count());
        assertEquals(1, batches.count());
    }
    @Test void missingAndPendingOrdersAreRejectedWithoutTickets() throws Exception {
        assertEquals(404, issue("missing").statusCode());
        assertEquals(409, issue("pending").statusCode());
        assertEquals(0, tickets.count());
        assertEquals(0, batches.count());
    }
    private HttpResponse<String> issue(String orderId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + port + "/internal/tickets/issue"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"orderId\":\"" + orderId + "\"}"))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
