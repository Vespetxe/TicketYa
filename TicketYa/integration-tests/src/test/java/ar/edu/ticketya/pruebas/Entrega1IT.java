package ar.edu.ticketya.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntFunction;
import org.junit.jupiter.api.*;

class Entrega1IT {
  static final int GATEWAY = 18080, INVENTARIO = 18082, ENTRADAS = 18083, INVENTARIO_2 = 18085;
  static Entorno red;

  @BeforeAll
  static void acreditar() throws Exception {
    red = Entorno.abrir();
  }

  record Evento(long id, long sector) {
    String ruta() {
      return "/api/v1/events/" + id;
    }

    String stock() {
      return "/disponibilidad/eventos/" + id + "/sectores/" + sector;
    }

    Map<String, Object> reserva(int cliente, int cantidad) {
      return Map.of("idCliente", cliente, "idEvento", id, "idSector", sector, "cantidad", cantidad);
    }
  }

  record Compra(long id, long reserva, Map<String, Object> pago, JsonNode resultado) {
    String ruta() {
      return "/compras/" + id;
    }

    String tickets() {
      return "/tickets?orderId=" + id;
    }

    String pagos() {
      return "/pagos?compraId=" + id;
    }
  }

  static String clave() {
    return UUID.randomUUID().toString();
  }

  static JsonNode ok(Entorno.Respuesta r) throws Exception {
    assertTrue(
        Set.of(200, 201).contains(r.statusCode()),
        () -> r.uri() + " HTTP " + r.statusCode() + " " + r.body());
    return Entorno.JSON.readTree(r.body());
  }

  static JsonNode get(String path) throws Exception {
    return ok(red.enviar(GATEWAY, "GET", path, null, false));
  }

  static JsonNode post(String path, Object body) throws Exception {
    return ok(red.enviar(GATEWAY, "POST", path, body, false));
  }

  static void patch(String path, boolean admin) throws Exception {
    ok(red.enviar(GATEWAY, "PATCH", path, null, admin));
  }

  static long lugar() throws Exception {
    return post(
            "/api/v1/venues",
            Map.of(
                "name",
                "Sala " + clave(),
                "address",
                "Calle 1",
                "city",
                "Tandil",
                "latitude",
                1,
                "longitude",
                1))
        .required("venueId")
        .longValue();
  }

  static long sector(long lugar, boolean numerado, int capacidad) throws Exception {
    var body =
        Entorno.JSON
            .createObjectNode()
            .put("name", "Sector " + clave())
            .put("capacity", capacidad)
            .put("type", numerado ? "NUMERADO" : "NO_NUMERADO");
    if (numerado) body.put("rows", 1).put("seatsPerRow", capacidad);
    return post("/api/v1/venues/" + lugar + "/sectors", body).required("sectorId").longValue();
  }

  static long borrador(long lugar) throws Exception {
    return post(
            "/api/v1/events",
            Map.of(
                "name",
                "Evento " + clave(),
                "description",
                "Prueba aislada",
                "eventType",
                "Recital",
                "organizerId",
                1,
                "venueId",
                lugar,
                "startDateTime",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(8).toString(),
                "participantIds",
                List.of()))
        .required("eventId")
        .longValue();
  }

  static Evento evento(boolean numerado, int capacidad, int precio) throws Exception {
    long lugar = lugar(), sector = sector(lugar, numerado, capacidad);
    var e = new Evento(borrador(lugar), sector);
    post(e.ruta() + "/prices", List.of(Map.of("sectorId", sector, "price", precio)));
    patch(e.ruta() + "/publish", false);
    patch(e.ruta() + "/publish", false);
    return e;
  }

  static Evento general() throws Exception {
    return evento(false, 2, 15000);
  }

  static JsonNode registrar(
      Evento e, int cliente, JsonNode reserva, List<Long> asientos, int cantidad) throws Exception {
    List<ObjectNode> detalles = new ArrayList<>();
    if (asientos.isEmpty())
      detalles.add(
          Entorno.JSON
              .createObjectNode()
              .put("sectorId", e.sector)
              .putNull("asientoId")
              .put("cantidad", cantidad));
    for (long asiento : asientos)
      detalles.add(
          Entorno.JSON
              .createObjectNode()
              .put("sectorId", e.sector)
              .put("asientoId", asiento)
              .put("cantidad", 1));
    return post(
        "/compras",
        Map.of(
            "clienteId",
            cliente,
            "eventoId",
            e.id,
            "reservaId",
            reserva.required("idReserva"),
            "detalles",
            detalles));
  }

  static Map<String, Object> pago(long id, String clave) {
    return Map.of("compraId", id, "claveIdempotencia", clave, "medioPago", "TARJETA_DEBITO");
  }

  static Compra comprar(Evento e, boolean numerado, String clave) throws Exception {
    int cantidad = numerado ? 2 : 1;
    List<Long> asientos = new ArrayList<>();
    if (numerado)
      for (var asiento : get(e.stock()).required("asientosLibres")) {
        asientos.add(asiento.required("idAsiento").longValue());
        if (asientos.size() == cantidad) break;
      }
    Map<String, Object> seleccion = new HashMap<>(e.reserva(1, cantidad));
    seleccion.put("idsAsientos", numerado ? asientos : null);
    var reserva = post("/reservas", seleccion);
    var compra = registrar(e, 1, reserva, asientos, cantidad);
    long id = compra.required("id").longValue();
    var pago = pago(id, clave);
    return new Compra(id, compra.required("reservaId").longValue(), pago, post("/pagos", pago));
  }

  static void estado(String esperado, JsonNode objeto) {
    assertEquals(esperado, objeto.required("estado").textValue());
  }

  static void stock(Evento e, int cantidad) throws Exception {
    assertEquals(cantidad, get(e.stock()).required("disponibles").intValue());
  }

  static void vencer(long id) throws Exception {
    var fecha = get("/reservas/" + id).required("fechaExpiracion").textValue();
    Duration espera =
        Duration.between(Instant.now(), LocalDateTime.parse(fecha).toInstant(ZoneOffset.UTC));
    assertTrue(espera.compareTo(Duration.ofSeconds(40)) < 0, "Se requiere TTL corto y JVM UTC");
    Thread.sleep((espera.isNegative() ? Duration.ZERO : espera).plusSeconds(2));
  }

  static String aprobar(Compra c, JsonNode entradas) throws Exception {
    var d =
        post(
            "/devoluciones",
            Map.of("compraId", c.id, "clienteId", 1, "entradasIds", entradas, "motivo", "Prueba"));
    String path = "/admin/devoluciones/" + d.required("id").longValue();
    post(path + "/aprobar", Map.of("motivo", "Aprobada"));
    return path + "/procesar";
  }

  static void compensado(Compra c) throws Exception {
    var actual = get(c.ruta());
    estado("FALLIDA", actual);
    assertEquals("FINALIZADA", actual.required("etapa").textValue());
  }

  static void reintegroTotal(Compra c) throws Exception {
    var pagos = get(c.pagos());
    assertEquals(1, pagos.size());
    estado("REINTEGRADO_TOTAL", pagos.get(0));
    assertEquals(15000, pagos.get(0).required("importeReintegrado").doubleValue());
  }

  static JsonNode porId(JsonNode tickets, JsonNode id) {
    return tickets
        .valueStream()
        .filter(t -> t.required("ticketId").equals(id))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void compraNumeradaYReintentos() throws Exception {
    var e = evento(true, 2, 15000);
    var c = comprar(e, true, clave());
    estado("APROBADO", c.resultado);
    estado("COMPLETADA", get(c.ruta()));
    var tickets = get(c.tickets());
    assertEquals(2, tickets.size());
    for (String campo : List.of("ticketId", "qr"))
      assertEquals(
          2, tickets.valueStream().map(t -> t.required(campo).textValue()).distinct().count());
    post("/pagos", c.pago);
    assertEquals(tickets, get(c.tickets()));
    stock(e, 0);
  }

  @Test
  void pagoRechazadoLibera() throws Exception {
    var e = evento(false, 2, 150000);
    var c = comprar(e, false, clave());
    estado("RECHAZADO", c.resultado);
    assertEquals(Entorno.JSON.createArrayNode(), get(c.tickets()));
    stock(e, 2);
  }

  @Test
  void timeoutPagoRecuperable() throws Exception {
    var c = comprar(general(), false, "simular-timeout-" + clave());
    estado("RESULTADO_DESCONOCIDO", c.resultado);
    post("/pagos", c.pago);
    estado("COMPLETADA", get(c.ruta()));
    assertEquals(1, get(c.tickets()).size());
  }

  @Test
  void pagoRecuperadoTrasVencimiento() throws Exception {
    var e = general();
    var c = comprar(e, false, "simular-timeout-" + clave());
    vencer(c.reserva);
    estado("REINTEGRADO_TOTAL", post("/pagos", c.pago));
    compensado(c);
    post("/pagos", c.pago);
    stock(e, 2);
    assertEquals(Entorno.JSON.createArrayNode(), get(c.tickets()));
  }

  @Test
  void devolucionParcialIdempotente() throws Exception {
    var e = evento(true, 2, 15000);
    var c = comprar(e, true, clave());
    get(c.ruta());
    var unidades = get(c.ruta() + "/entradas");
    assertEquals(2, unidades.size());
    var elegida =
        unidades
            .valueStream()
            .max(Comparator.comparingLong(u -> u.required("asientoId").longValue()))
            .orElseThrow();
    var otra =
        unidades
            .valueStream()
            .filter(u -> !u.required("entradaId").equals(elegida.required("entradaId")))
            .findFirst()
            .orElseThrow();
    assertEquals(
        elegida.required("asientoId").asText(),
        porId(get(c.tickets()), elegida.required("ticketId")).required("seatId").textValue());
    String path = aprobar(c, Entorno.JSON.createArrayNode().add(elegida.required("entradaId")));
    var body = Map.of("claveIdempotencia", clave(), "importe", 15000);
    assertEquals("FINALIZADA", post(path, body).required("etapa").textValue());
    post(path, body);
    stock(e, 1);
    var tickets = get(c.tickets());
    assertEquals(
        1,
        tickets.valueStream().filter(t -> t.required("status").asText().equals("ANULADA")).count());
    assertEquals(
        "ANULADA", porId(tickets, elegida.required("ticketId")).required("status").textValue());
    assertEquals(
        "EMITIDA", porId(tickets, otra.required("ticketId")).required("status").textValue());
    var libres = get(e.stock()).required("asientosLibres");
    assertEquals(
        List.of(elegida.required("asientoId")),
        libres.valueStream().map(s -> s.required("idAsiento")).toList());
    assertEquals(15000, get(c.pagos()).required(0).required("importeReintegrado").doubleValue());
  }

  @Test
  void cancelacionReintegraYBloquea() throws Exception {
    var e = general();
    var c = comprar(e, false, clave());
    patch(e.ruta() + "/cancel", true);
    patch(e.ruta() + "/cancel", true);
    stock(e, 0);
    assertEquals(
        409, red.enviar(GATEWAY, "POST", "/reservas", e.reserva(2, 1), false).statusCode());
    var tickets = get(c.tickets());
    assertEquals(1, tickets.size());
    assertEquals("ANULADA", tickets.required(0).required("status").textValue());
    reintegroTotal(c);
  }

  static <T> List<T> simultaneos(int cantidad, IntFunction<Callable<T>> operacion)
      throws Exception {
    var inicio = new CyclicBarrier(cantidad);
    try (var pool = Executors.newFixedThreadPool(cantidad)) {
      List<Future<T>> futuros = new ArrayList<>();
      for (int i = 0; i < cantidad; i++) {
        var tarea = operacion.apply(i);
        futuros.add(
            pool.submit(
                () -> {
                  inicio.await(10, TimeUnit.SECONDS);
                  return tarea.call();
                }));
      }
      List<T> resultados = new ArrayList<>();
      for (var futuro : futuros) resultados.add(futuro.get(45, TimeUnit.SECONDS));
      return resultados;
    }
  }

  @Test
  void cancelacionConcurrenteDePagoIncierto() throws Exception {
    var e = general();
    var c = comprar(e, false, "simular-timeout-" + clave());
    estado("RESULTADO_DESCONOCIDO", c.resultado);
    var codigos =
        simultaneos(
            2,
            i -> () -> red.enviar(GATEWAY, "PATCH", e.ruta() + "/cancel", null, true).statusCode());
    assertTrue(codigos.stream().allMatch(Set.of(200, 503)::contains), codigos.toString());
    patch(e.ruta() + "/cancel", true);
    compensado(c);
    reintegroTotal(c);
    assertEquals(Entorno.JSON.createArrayNode(), get(c.tickets()));
    stock(e, 0);
    assertEquals(
        409,
        red.enviar(
                ENTRADAS,
                "POST",
                "/internal/tickets/issue",
                Map.of("orderId", String.valueOf(c.id)),
                false)
            .statusCode());
  }

  @Test
  void diezSolicitudesDosInstanciasUltimoCupo() throws Exception {
    var e = evento(false, 1, 15000);
    var respuestas =
        simultaneos(
            10,
            i ->
                () ->
                    red.enviar(
                        i % 2 == 0 ? GATEWAY : INVENTARIO_2,
                        "POST",
                        "/reservas",
                        e.reserva(100 + i, 1),
                        false));
    for (int i = 0; i < respuestas.size(); i++) {
      var respuesta = respuestas.get(i);
      System.out.printf("Cliente %d -> %s HTTP %d%n", 100 + i, respuesta.uri(), respuesta.statusCode());
    }
    assertEquals(1, respuestas.stream().filter(r -> r.statusCode() == 201).count());
    assertEquals(9, respuestas.stream().filter(r -> r.statusCode() == 409).count());
    int ganador = 0;
    while (respuestas.get(ganador).statusCode() != 201) ganador++;
    var compra = registrar(e, 100 + ganador, ok(respuestas.get(ganador)), List.of(), 1);
    long id = compra.required("id").longValue();
    estado("APROBADO", post("/pagos", pago(id, clave())));
    estado("COMPLETADA", get("/compras/" + id));
    stock(e, 0);
  }

  @Test
  void suspensionBloqueaReservas() throws Exception {
    var e = general();
    patch(e.ruta() + "/suspend", true);
    assertEquals(
        409, red.enviar(GATEWAY, "POST", "/reservas", e.reserva(2, 1), false).statusCode());
  }

  @Test
  void anulacionImpideEmisionTardia() throws Exception {
    String orden = "barrera-" + clave();
    assertEquals(
        200,
        red.enviar(ENTRADAS, "POST", "/internal/tickets/orders/" + orden + "/annul", null, false)
            .statusCode());
    assertEquals(
        409,
        red.enviar(ENTRADAS, "POST", "/internal/tickets/issue", Map.of("orderId", orden), false)
            .statusCode());
  }

  @Test
  void reservaVencidaNoPuedePagarse() throws Exception {
    var e = general();
    var reserva = post("/reservas", e.reserva(1, 1));
    var compra = registrar(e, 1, reserva, List.of(), 1);
    vencer(reserva.required("idReserva").longValue());
    long id = compra.required("id").longValue();
    assertEquals(400, red.enviar(GATEWAY, "POST", "/pagos", pago(id, clave()), false).statusCode());
    stock(e, 2);
    assertEquals(Entorno.JSON.createArrayNode(), get("/tickets?orderId=" + id));
  }

  @Test
  void busquedaCiudadYGatewayInterno() throws Exception {
    var e = general();
    var eventos = get("/api/v1/events/search?city=Tandil&type=Recital&sort=RELEVANCE");
    assertTrue(eventos.valueStream().anyMatch(v -> v.required("eventId").longValue() == e.id));
    assertEquals(Entorno.JSON.createArrayNode(), get("/api/v1/events/search?city=NoExiste"));
    assertEquals(404, red.enviar(GATEWAY, "GET", "/internal/orders/1", null, false).statusCode());
  }

  @Test
  void relevanciaYDevolucionDeAgotado() throws Exception {
    var e = evento(false, 1, 15000);
    var c = comprar(e, false, clave());
    patch(e.ruta() + "/status?status=AGOTADO", true);
    var otro = general();
    var ids =
        get("/api/v1/events/search?sort=RELEVANCE")
            .valueStream()
            .map(v -> v.required("eventId").longValue())
            .toList();
    assertTrue(ids.containsAll(List.of(otro.id, e.id)) && ids.indexOf(otro.id) < ids.indexOf(e.id));
    String path = aprobar(c, get(c.ruta()).required("entradasIds"));
    var r =
        post(path, Map.of("claveIdempotencia", clave(), "importe", 15000)).required("reintegro");
    estado("COMPLETADO", r);
    assertEquals(15000, r.required("importe").doubleValue());
    stock(e, 1);
    patch(e.ruta() + "/status?status=PUBLICADO", true);
    patch(e.ruta() + "/status?status=FINALIZADO", true);
    assertEquals(
        409, red.enviar(GATEWAY, "POST", "/reservas", e.reserva(2, 1), false).statusCode());
  }

  @Test
  void publicacionReanudaInventarioParcial() throws Exception {
    long lugar = lugar(), s1 = sector(lugar, false, 2), s2 = sector(lugar, false, 2);
    var e = new Evento(borrador(lugar), s1);
    post(
        e.ruta() + "/prices",
        List.of(Map.of("sectorId", s1, "price", 100), Map.of("sectorId", s2, "price", 100)));
    var body =
        Map.of("idSector", s1, "esNumerado", false, "capacidadTotal", 2, "asientos", List.of());
    assertEquals(
        201,
        red.enviar(INVENTARIO, "POST", "/eventos/" + e.id + "/inventario", body, false)
            .statusCode());
    assertEquals("BORRADOR", get(e.ruta()).required("status").textValue());
    assertEquals(
        409, red.enviar(GATEWAY, "POST", "/reservas", e.reserva(99, 1), false).statusCode());
    var otro = new Evento(e.id, s2);
    assertEquals(404, red.enviar(GATEWAY, "GET", otro.stock(), null, false).statusCode());
    patch(e.ruta() + "/publish", false);
    patch(e.ruta() + "/publish", false);
    stock(e, 2);
    stock(otro, 2);
  }
}
