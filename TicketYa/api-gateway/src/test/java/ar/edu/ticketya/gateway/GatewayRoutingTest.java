package ar.edu.ticketya.gateway;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {
    static HttpServer upstream;
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry)throws Exception {
        upstream=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        upstream.createContext("/",exchange->{byte[] body=exchange.getRequestURI().toString().getBytes();exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
        upstream.start();
        for(String name:new String[]{"eventos","inventario","entradas","compras"})registry.add("ticketya."+name+".url",()->"http://127.0.0.1:"+upstream.getAddress().getPort());
    }
    @AfterAll static void close(){upstream.stop(0);}
    HttpResponse<String> get(String path)throws Exception{return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    @Test void enrutaLosCuatroServiciosSinCambiarRuta()throws Exception{
        for(String path:new String[]{"/api/v1/events/search?city=Tandil","/reservas/1","/tickets?clientId=1","/compras/1"}){
            var result=get(path);assertEquals(200,result.statusCode());assertEquals(path,result.body());
        }
    }
    @Test void noPublicaOperacionesInternas()throws Exception{
        for(String path:new String[]{"/internal/orders/1","/internal/tickets/issue","/reservas/1/confirmar","/ocupaciones/liberar","/eventos/1/inventario"})assertEquals(404,get(path).statusCode());
    }
}
