package ar.edu.tudai.ticketya.servicio_entradas.integration;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class RestComprasOrderClientTest {
    private HttpServer server;
    private RestComprasOrderClient client;
    @BeforeEach void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        client = new RestComprasOrderClient(RestClient.builder(),
                "http://localhost:" + server.getAddress().getPort());
    }
    @AfterEach void stopServer() { server.stop(0); }
    @Test void readsConfirmedOrderAndItsItems() {
        respond(200, """
                {"orderId":"order-1","clientId":"client-1","status":"CONFIRMADA",
                 "items":[{"eventId":"event-1","sectorId":"campo","seatId":null,"quantity":2}]}
                """);
        OrderSnapshot order = client.getOrder("order-1");
        assertEquals("client-1", order.clientId());
        assertEquals("campo", order.items().getFirst().sectorId());
        assertEquals(2, order.items().getFirst().quantity());
    }
    @Test void rejectsAnOrderThatDoesNotExist() {
        respond(404, "{}");
        assertThrows(OrderNotFoundException.class, () -> client.getOrder("missing"));
    }
    @Test void rejectsAnOrderThatIsNotConfirmed() {
        respond(200, """
                {"orderId":"order-1","clientId":"client-1","status":"PENDIENTE",
                 "items":[{"eventId":"event-1","sectorId":"campo","seatId":null,"quantity":1}]}
                """);
        assertThrows(OrderNotConfirmedException.class, () -> client.getOrder("order-1"));
    }
    private void respond(int status, String body) {
        server.createContext("/internal/orders/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
}
