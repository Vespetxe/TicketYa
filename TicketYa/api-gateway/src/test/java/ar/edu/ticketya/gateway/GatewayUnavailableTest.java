package ar.edu.ticketya.gateway;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="ticketya.entradas.url=http://127.0.0.1:1")
class GatewayUnavailableTest {
    @Value("${local.server.port}") int port;
    @Test void servicioCaidoDevuelve503()throws Exception {
        var result=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/tickets?clientId=1")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(503,result.statusCode());assertTrue(result.body().contains("Servicio no disponible"));
    }
}
