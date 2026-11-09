package ar.edu.ticketya.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import java.net.ConnectException;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// El lanzador selecciona explícitamente esta prueba después de iniciar cinco procesos.
class AcreditacionParcial {
  @Test
  void abortaAntesDeEscribirCuandoFaltaInventarioDos() {
    var cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    var puertos = new ArrayList<Integer>();
    assertThrows(
        ConnectException.class,
        () ->
            Entorno.acreditar(
                10000,
                System.getenv("TICKETYA_TEST_TOKEN"),
                req -> {
                  assertEquals("GET", req.method());
                  puertos.add(req.uri().getPort());
                  var respuesta = cliente.send(req, HttpResponse.BodyHandlers.ofString());
                  return new Entorno.Respuesta(
                      respuesta.statusCode(), respuesta.uri(), respuesta.body());
                }));
    assertEquals(List.of(28080, 28081, 28082, 28083, 28084, 28085), puertos);
  }
}
