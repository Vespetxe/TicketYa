package ar.edu.ticketya.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class EntornoTest {
  private static final String TOKEN = "x".repeat(32);
  private final List<HttpRequest> llamadas = new ArrayList<>();
  private int codigo = 200;

  interface Alteracion {
    void aplicar(ObjectNode datos, int puerto);
  }

  private Entorno verificar(String token, int offset, Alteracion cambio) throws Exception {
    return Entorno.acreditar(
        offset,
        token,
        req -> {
          assertEquals("GET", req.method());
          assertEquals("/internal/test-environment", req.uri().getPath());
          assertEquals("127.0.0.1", req.uri().getHost());
          assertEquals(TOKEN, req.headers().firstValue("X-TicketYa-Test-Token").orElseThrow());
          int puerto = req.uri().getPort() - offset;
          var servicio = Entorno.SERVICIOS.get(llamadas.size());
          assertEquals(servicio.puerto(), puerto);
          llamadas.add(req);
          var datos = Entorno.JSON.createObjectNode().put("service", servicio.nombre());
          if (puerto != 18080) datos.put("database", "ticketya_e1_" + servicio.nombre());
          servicio
              .destinos()
              .forEach(
                  (nombre, destino) -> datos.put(nombre, "http://127.0.0.1:" + (destino + offset)));
          cambio.aplicar(datos, puerto);
          return new Entorno.Respuesta(codigo, req.uri(), datos.toString());
        });
  }

  @Test
  void tokenAusente() {
    assertThrows(IllegalStateException.class, () -> verificar(null, 0, (d, p) -> {}));
    assertTrue(llamadas.isEmpty());
  }

  @Test
  void redInvalida() {
    assertThrows(IllegalStateException.class, () -> verificar(TOKEN, 1, (d, p) -> {}));
    assertTrue(llamadas.isEmpty());
  }

  @Test
  void baseNormal() {
    assertThrows(
        IllegalStateException.class,
        () ->
            verificar(
                TOKEN,
                0,
                (d, p) -> {
                  if (p == 18081) d.put("database", "desarrollo");
                }));
    assertEquals(2, llamadas.size());
  }

  static Stream<Arguments> destinos() {
    // Esta matriz es independiente de la implementación: quitar una ruta de la
    // guarda debe hacer fallar su prueba negativa.
    var esperados = Map.of(
        18080, List.of("eventos", "inventario", "entradas", "compras"),
        18081, List.of("inventario", "entradas", "compras"),
        18083, List.of("compras"),
        18084, List.of("eventos", "inventario", "entradas"));
    return esperados.entrySet().stream().flatMap(servicio -> servicio.getValue().stream()
        .flatMap(clave -> Stream.of(null, "http://localhost:8082")
            .map(valor -> Arguments.of(servicio.getKey(), clave, valor))));
  }

  @ParameterizedTest
  @MethodSource("destinos")
  void destinoIncorrecto(int puerto, String clave, String valor) {
    assertThrows(
        IllegalStateException.class,
        () ->
            verificar(
                TOKEN,
                0,
                (d, p) -> {
                  if (p == puerto) {
                    if (valor == null) d.remove(clave);
                    else d.put(clave, valor);
                  }
                }));
    assertEquals(puerto - 18080 + 1, llamadas.size());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 10000})
  void seisProcesosValidos(int offset) throws Exception {
    verificar(TOKEN, offset, (d, p) -> {});
    assertEquals(6, llamadas.size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void segundaInstanciaObligatoria(boolean ausente) {
    assertThrows(
        IllegalStateException.class,
        () ->
            verificar(
                TOKEN,
                0,
                (d, p) -> {
                  if (p == 18085) {
                    if (ausente) codigo = 503;
                    else d.put("database", "otra");
                  }
                }));
    assertEquals(6, llamadas.size());
  }
}
