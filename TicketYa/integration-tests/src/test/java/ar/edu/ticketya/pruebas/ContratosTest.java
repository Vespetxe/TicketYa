package ar.edu.ticketya.pruebas;

import static ar.edu.ticketya.pruebas.GenerarDocumentacionTest.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;

class ContratosTest {
  // Las descripciones y los ejemplos de negocio revisados viven en los contratos de
  // Entrega 1. La estructura de los DTO y los ejemplos restantes se derivan del insumo.
  private static final Set<String> EJEMPLOS_DE_DOMINIO =
      Set.of(
          "EntradaSeleccionable",
          "VenueRequestDTO",
          "ParticipantRequestDTO",
          "SectorRequestDTO",
          "EventRequestDTO",
          "EventSectorPriceRequestDTO",
          "CrearReservaRequest",
          "InicializarInventarioRequest",
          "CrearCompraDTO",
          "ProcesarPagoDTO",
          "SolicitarDevolucionDTO",
          "AprobarDevolucionDTO",
          "RechazarDevolucionDTO",
          "ProcesarDevolucionDTO",
          "DevolverLugaresRequest",
          "LiberarDevolucionRequest",
          "IssueTicketsRequest",
          "TicketStatusRequest",
          "OrderEventStatusRequest",
          "Ticket",
          "Order",
          "MensajeResponse");

  static void recogerEjemplos(JsonNode nodo, Map<String, JsonNode> ejemplos) {
    if (nodo.has("schema") && nodo.has("example"))
      recogerEjemplo(nodo.get("schema"), nodo.get("example"), ejemplos);
    nodo.forEach(child -> recogerEjemplos(child, ejemplos));
  }

  static void recogerEjemplo(JsonNode schema, JsonNode valor, Map<String, JsonNode> ejemplos) {
    if (schema.has("$ref")) {
      String ref = schema.get("$ref").asText();
      String nombre = ref.substring(ref.lastIndexOf('/') + 1);
      if (EJEMPLOS_DE_DOMINIO.contains(nombre)) ejemplos.putIfAbsent(nombre, valor);
    } else if (schema.has("items") && valor.isArray() && !valor.isEmpty()) {
      recogerEjemplo(schema.get("items"), valor.get(0), ejemplos);
    }
  }

  static JsonNode ejemplo(
      JsonNode schema, JsonNode schemas, Map<String, JsonNode> ejemplos, Set<String> vistos) {
    if (schema.has("$ref")) {
      String ref = schema.get("$ref").asText();
      String nombre = ref.substring(ref.lastIndexOf('/') + 1);
      if (ejemplos.containsKey(nombre)) return ejemplos.get(nombre);
      if (vistos.contains(nombre)) return NullNode.instance;
      var cadena = new HashSet<>(vistos);
      cadena.add(nombre);
      return ejemplo(schemas.required(nombre), schemas, ejemplos, cadena);
    }
    if (schema.has("example")) return schema.get("example");
    if (schema.has("enum")) return schema.get("enum").required(0);
    var t = schema.path("type");
    String tipo =
        t.isArray()
            ? t.valueStream()
                .map(JsonNode::asText)
                .filter(v -> !v.equals("null"))
                .findFirst()
                .orElse("string")
            : t.asText("object");
    return switch (tipo) {
      case "array" ->
          JSON.createArrayNode().add(ejemplo(schema.path("items"), schemas, ejemplos, vistos));
      case "object" -> {
        var obj = JSON.createObjectNode();
        schema
            .path("properties")
            .properties()
            .forEach(e -> obj.set(e.getKey(), ejemplo(e.getValue(), schemas, ejemplos, vistos)));
        yield obj;
      }
      case "boolean" -> BooleanNode.TRUE;
      case "integer", "number" ->
          schema.has("minimum") ? schema.get("minimum") : IntNode.valueOf(1);
      default ->
          TextNode.valueOf(
              switch (schema.path("format").asText()) {
                case "date-time" -> "2030-12-01T21:00:00Z";
                case "uuid" -> "00000000-0000-4000-8000-000000000001";
                default -> "ejemplo";
              });
    };
  }

  static void ejemplosDeContenido(
      JsonNode contenido, JsonNode schemas, Map<String, JsonNode> ejemplos) {
    for (var media : contenido)
      if (media.has("schema"))
        ((ObjectNode) media)
            .set("example", ejemplo(media.get("schema"), schemas, ejemplos, Set.of()));
  }

  static ObjectNode aplicarContrato(ObjectNode exportado, JsonNode revisado) {
    var copia = exportado.deepCopy();
    var schemas = copia.withObject("/components/schemas");
    var referencia = revisado.required("components").required("schemas");
    for (String nuevo : List.of("ErrorHTTP", "ErrorEvento", "ErrorResponse", "SeatResponseDTO"))
      if (referencia.has(nuevo)) schemas.set(nuevo, referencia.get(nuevo));
    for (var par : schemas.properties()) {
      var schema = (ObjectNode) par.getValue();
      var base = referencia.path(par.getKey());
      if (par.getKey().equals("EntradaSeleccionable"))
        schema.set("description", base.required("description"));
      for (var campo : schema.path("properties").properties()) {
        var prop = (ObjectNode) campo.getValue();
        var modelo = base.path("properties").path(campo.getKey());
        if (modelo.path("type").isArray() && prop.path("type").isTextual())
          prop.set("type", modelo.get("type"));
        if (campo.getKey().equals("ticketId") && par.getKey().equals("EntradaSeleccionable"))
          prop.put("format", "uuid");
        if ((campo.getKey().startsWith("fecha") || campo.getKey().equals("createdAt"))
            && !modelo.has("format")
            && modelo.has("example")
            && modelo.has("description")) {
          prop.remove("format");
          prop.set("example", modelo.get("example"));
          prop.set("description", modelo.get("description"));
        }
      }
    }
    var ejemplos = new HashMap<String, JsonNode>();
    recogerEjemplos(revisado, ejemplos);
    if (revisado.has("tags")) copia.set("tags", revisado.get("tags"));
    var paths = copia.required("paths");
    for (var ruta : revisado.required("paths").properties()) {
      var metodos = paths.required(ruta.getKey());
      for (var metodo : ruta.getValue().properties()) {
        var op = (ObjectNode) metodos.required(metodo.getKey());
        var base = metodo.getValue();
        for (String campo :
            List.of("description", "summary", "parameters", "tags", "operationId", "x-visibility"))
          if (base.has(campo)) op.set(campo, base.get(campo).deepCopy());
        var respuestas = op.withObject("/responses");
        respuestas.retain(
            base.required("responses").propertyStream().map(Map.Entry::getKey).toList());
        for (var r : base.required("responses").properties()) {
          // Los errores y los esquemas de respuesta corregidos se describen por endpoint.
          var respuesta = (ObjectNode) r.getValue().deepCopy();
          if (r.getKey().startsWith("2"))
            ejemplosDeContenido(respuesta.path("content"), schemas, ejemplos);
          respuestas.set(r.getKey(), respuesta);
        }
        ejemplosDeContenido(op.path("requestBody").path("content"), schemas, ejemplos);
      }
    }
    return copia;
  }

  static boolean publico(String servicio, String ruta, String metodo) {
    return switch (servicio) {
      case "eventos" -> ruta.startsWith("/api/v1/");
      case "compras" -> !ruta.startsWith("/internal/");
      case "entradas" -> metodo.equals("get") && ruta.equals("/tickets");
      case "inventario" ->
          ruta.equals("/reservas") && Set.of("get", "post").contains(metodo)
              || metodo.equals("get")
                  && (ruta.equals("/reservas/{id}") || ruta.startsWith("/disponibilidad/"));
      default -> throw new IllegalArgumentException(servicio);
    };
  }

  static JsonNode prefijar(JsonNode input, String servicio) {
    var copia = input.deepCopy();
    prefijarEn(copia, servicio);
    return copia;
  }

  private static void prefijarEn(JsonNode n, String servicio) {
    if (n instanceof ObjectNode obj && obj.has("$ref")) {
      String ref = obj.get("$ref").asText();
      if (ref.startsWith("#/components/schemas/"))
        obj.put(
            "$ref", ref.replace("#/components/schemas/", "#/components/schemas/" + servicio + "_"));
    }
    n.forEach(child -> prefijarEn(child, servicio));
  }

  @Test
  void normalizarContratosYGenerarGateway() throws Exception {
    var gateway = JSON.createObjectNode().put("openapi", "3.1.0");
    gateway
        .putObject("info")
        .put("title", "TicketYa - API publica del Gateway")
        .put("version", "entrega-1")
        .put(
            "description",
            "Rutas efectivamente publicadas por GatewayApplication. Sin JWT en Entrega 1. Los errores de negocio se conservan; un proveedor inaccesible produce 503. No publica /internal/** ni mutaciones directas de inventario.");
    gateway.putArray("servers").addObject().put("url", "http://localhost:8080");
    var rutas = gateway.putObject("paths");
    var esquemas = gateway.putObject("components").putObject("schemas");
    var filas = new StringBuilder();
    var noDisponible =
        JSON.readTree(
            """
            {"description":"Proveedor no disponible o recuperacion pendiente; no generar otra identidad de operacion",
            "content":{"application/json":{"schema":{"type":"object","additionalProperties":true},
            "example":{"status":503,"message":"Servicio no disponible"}}}}
            """);
    for (var servicio : SERVICIOS) {
      var revisado = leer(DOC.resolve("openapi/" + servicio + ".json"));
      Path origen =
          servicio.equals("entradas")
              ? ROOT.resolve("TicketYa/entradas_service/docs/openapi.yaml")
              : Path.of(System.getProperty("ticketya.openapi", DOC.resolve("openapi").toString()))
                  .resolve(servicio + ".json");
      var documento = aplicarContrato((ObjectNode) leer(origen), revisado);
      for (var schema : documento.at("/components/schemas").properties())
        esquemas.set(servicio + "_" + schema.getKey(), prefijar(schema.getValue(), servicio));
      var paths = documento.required("paths");
      for (String ruta : new TreeSet<>(paths.propertyStream().map(Map.Entry::getKey).toList())) {
        for (var entry : paths.get(ruta).properties()) {
          var op = (ObjectNode) entry.getValue();
          String metodo = entry.getKey();
          boolean visible = publico(servicio, ruta, metodo);
          String visibilidad =
              ruta.startsWith("/admin/") ? "administrativa" : visible ? "publica" : "interna";
          op.put("x-visibility", visibilidad);
          op.put(
              "operationId",
              servicio
                  + "_"
                  + metodo
                  + "_"
                  + ruta.substring(1)
                      .replace('/', '_')
                      .replace("{", "")
                      .replace("}", "")
                      .replace('-', '_'));
          var codigos =
              new TreeSet<>(
                  op.required("responses").propertyStream().map(Map.Entry::getKey).toList());
          filas
              .append("| ")
              .append(servicio)
              .append(" | ")
              .append(metodo.toUpperCase(Locale.ROOT))
              .append(" | ")
              .append((char) 96)
              .append(ruta)
              .append((char) 96)
              .append(" | ")
              .append(visibilidad)
              .append(" | ")
              .append(String.join(", ", codigos))
              .append(" |\n");
          if (visible) {
            var publicacion = (ObjectNode) prefijar(op, servicio);
            ((ObjectNode) publicacion.required("responses")).set("503", noDisponible);
            rutas.withObjectProperty(ruta).set(metodo, publicacion);
          }
        }
      }
      compararJson("openapi/" + servicio + ".json", documento);
    }
    compararJson("openapi/gateway.json", gateway);
    String indice = texto(DOC.resolve("openapi/README.md"));
    String resultado =
        indice.replaceFirst(
            "(?m)(?:^\\| (?:eventos|inventario|compras|entradas) \\|[^\\n]*\\n)+",
            Matcher.quoteReplacement(filas.toString()));
    compararTexto("openapi/README.md", resultado);
  }

  @Test
  void recuperarCorreccionesSobreUnaCopia() throws Exception {
    var revisado = leer(DOC.resolve("openapi/compras.json"));
    var exportacion = (ObjectNode) revisado.deepCopy();
    var operacion =
        (ObjectNode)
            exportacion
                .required("paths")
                .required("/admin/devoluciones/{id}/aprobar")
                .required("post");
    operacion.remove(List.of("parameters", "responses", "description"));
    exportacion.withObject("/components/schemas").remove("ErrorHTTP");
    assertEquals(revisado, aplicarContrato(exportacion, revisado));
    assertFalse(exportacion.at("/components/schemas").has("ErrorHTTP"));
  }
}
