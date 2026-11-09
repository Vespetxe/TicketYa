package ar.edu.ticketya.pruebas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

final class Entorno {
  static final ObjectMapper JSON = new ObjectMapper();

  record Servicio(String nombre, int puerto, Map<String, Integer> destinos) {}

  static final List<Servicio> SERVICIOS =
      List.of(
          new Servicio(
              "gateway",
              18080,
              Map.of("eventos", 18081, "inventario", 18082, "entradas", 18083, "compras", 18084)),
          new Servicio(
              "eventos", 18081, Map.of("inventario", 18082, "entradas", 18083, "compras", 18084)),
          new Servicio("inventario", 18082, Map.of()),
          new Servicio("entradas", 18083, Map.of("compras", 18084)),
          new Servicio(
              "compras", 18084, Map.of("eventos", 18081, "inventario", 18082, "entradas", 18083)),
          new Servicio("inventario", 18085, Map.of()));

  record Respuesta(int statusCode, URI uri, String body) {}

  interface Transporte {
    Respuesta enviar(HttpRequest solicitud) throws Exception;
  }

  private final int offset;
  private final Transporte transporte;

  private Entorno(int offset, Transporte transporte) {
    this.offset = offset;
    this.transporte = transporte;
  }

  static Entorno abrir() throws Exception {
    var cliente =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    return acreditar(
        Integer.parseInt(System.getenv().getOrDefault("TICKETYA_TEST_PORT_OFFSET", "0")),
        System.getenv("TICKETYA_TEST_TOKEN"),
        req -> {
          var res = cliente.send(req, HttpResponse.BodyHandlers.ofString());
          return new Respuesta(res.statusCode(), res.uri(), res.body());
        });
  }

  static Entorno acreditar(int offset, String token, Transporte transporte) throws Exception {
    if ((offset != 0 && offset != 10000) || token == null || token.length() < 32)
      throw new IllegalStateException("Red o token de prueba inválido");
    var entorno = new Entorno(offset, transporte);
    for (var servicio : SERVICIOS) {
      var req =
          entorno
              .peticion(servicio.puerto(), "/internal/test-environment")
              .header("X-TicketYa-Test-Token", token)
              .timeout(Duration.ofSeconds(5))
              .GET()
              .build();
      var response = transporte.enviar(req);
      if (response.statusCode() != 200)
        throw new IllegalStateException("Acreditación HTTP " + req.uri());
      JsonNode datos = JSON.readTree(response.body());
      exigir(datos, "service", servicio.nombre());
      if (!servicio.nombre().equals("gateway"))
        exigir(datos, "database", "ticketya_e1_" + servicio.nombre());
      for (var destino : servicio.destinos().entrySet())
        exigir(datos, destino.getKey(), base(destino.getValue() + offset));
    }
    return entorno;
  }

  private static void exigir(JsonNode data, String clave, String valor) {
    if (!valor.equals(data.path(clave).textValue()))
      throw new IllegalStateException("Acreditación inválida: " + clave);
  }

  private static String base(int puerto) {
    return "http://127.0.0.1:" + puerto;
  }

  private HttpRequest.Builder peticion(int puerto, String path) {
    if (SERVICIOS.stream().noneMatch(s -> s.puerto() == puerto) || !path.startsWith("/"))
      throw new IllegalArgumentException("Destino fuera de la red aislada");
    return HttpRequest.newBuilder(URI.create(base(puerto + offset) + path))
        .timeout(Duration.ofSeconds(25));
  }

  Respuesta enviar(int puerto, String method, String path, Object body, boolean admin)
      throws Exception {
    var builder = peticion(puerto, path).header("Content-Type", "application/json");
    if (path.startsWith("/admin/") || admin) builder.header("X-Admin-Id", "7");
    if (admin) builder.header("X-User-Role", "ADMIN");
    var payload =
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body));
    return transporte.enviar(builder.method(method, payload).build());
  }
}
