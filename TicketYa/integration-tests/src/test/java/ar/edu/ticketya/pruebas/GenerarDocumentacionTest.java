package ar.edu.ticketya.pruebas;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import org.junit.jupiter.api.Test;

class GenerarDocumentacionTest {
  static final ObjectMapper JSON = new ObjectMapper();
  static final Path ROOT = Path.of("../..").toAbsolutePath().normalize();
  static final Path DOC = ROOT.resolve("docs/entrega1");
  static final Path OUT = Path.of("target/documentacion");
  static final List<String> SERVICIOS = List.of("eventos", "inventario", "compras", "entradas");

  static JsonNode leer(Path p) throws Exception {
    return JSON.readTree(p.toFile());
  }

  static String texto(Path p) throws Exception {
    return Files.readString(p).replace("\r\n", "\n");
  }

  static void guardar(String ruta, String texto) throws Exception {
    Path p = OUT.resolve(ruta);
    Files.createDirectories(p.getParent());
    Files.writeString(p, texto);
  }

  static void compararJson(String ruta, JsonNode datos) throws Exception {
    guardar(ruta, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(datos) + "\n");
    assertEquals(leer(DOC.resolve(ruta)), leer(OUT.resolve(ruta)), ruta);
  }

  static void compararTexto(String ruta, String datos) throws Exception {
    guardar(ruta, datos);
    assertEquals(texto(DOC.resolve(ruta)), texto(OUT.resolve(ruta)), ruta);
  }

  static void script(ArrayNode events, String tipo, List<String> lineas) {
    var script = events.addObject().put("listen", tipo).putObject("script");
    script.put("type", "text/javascript").set("exec", JSON.valueToTree(lineas));
  }

  static List<String> lineas(JsonNode json) {
    return json.valueStream().map(JsonNode::textValue).toList();
  }

  static void solicitud(ArrayNode items, JsonNode paso) {
    String ruta = paso.required("ruta").textValue(), nombre = paso.required("nombre").textValue();
    var item = items.addObject().put("name", nombre);
    var req =
        item.putObject("request")
            .put("method", paso.required("metodo").textValue())
            .put("description", nombre)
            .put("url", "{{base_url}}" + ruta);
    var headers = req.putArray("header");
    if (paso.has("cuerpo")) {
      headers.addObject().put("key", "Content-Type").put("value", "application/json");
      var body =
          req.putObject("body")
              .put("mode", "raw")
              .put("raw", String.join("\n", lineas(paso.get("cuerpo"))));
      body.putObject("options").putObject("raw").put("language", "json");
    }
    if (paso.path("admin").asBoolean())
      headers.addObject().put("key", "X-User-Role").put("value", "ADMIN");
    if (paso.path("admin").asBoolean() || ruta.startsWith("/admin/"))
      headers.addObject().put("key", "X-Admin-Id").put("value", "{{administrador_id}}");
    var eventos = item.putArray("event");
    if (paso.has("previo")) script(eventos, "prerequest", lineas(paso.get("previo")));
    String codigos = paso.required("codigos").toString().replace(",", ", ");
    var test =
        new ArrayList<>(
            List.of(
                "pm.test(\"HTTP esperado\", () => pm.expect("
                    + codigos
                    + ").to.include(pm.response.code));",
                "if (" + codigos + ".includes(pm.response.code)) {"));
    if (paso.has("verificar")) test.addAll(lineas(paso.get("verificar")));
    else test.add("");
    test.add("}");
    script(eventos, "test", test);
  }

  @Test
  void generarColeccionYEntorno() throws Exception {
    JsonNode spec;
    try (var in = getClass().getResourceAsStream("/postman-entrega1.json")) {
      spec = JSON.readTree(in);
    }
    var coleccion = JSON.createObjectNode();
    coleccion.set("info", spec.required("info"));
    var grupos = coleccion.putArray("item");
    for (var g : spec.required("carpetas")) {
      var carpeta =
          grupos
              .addObject()
              .put("name", g.required("nombre").textValue())
              .put("description", spec.required("descripcion").textValue());
      var items = carpeta.putArray("item");
      for (var id : g.required("pasos"))
        solicitud(items, spec.required("operaciones").required(id.textValue()));
    }
    var entorno =
        JSON.createObjectNode()
            .put("name", spec.required("entorno").textValue())
            .put("_postman_variable_scope", "environment");
    var values = entorno.putArray("values");
    spec.required("variables")
        .properties()
        .forEach(
            entry ->
                values
                    .addObject()
                    .put("key", entry.getKey())
                    .put("value", entry.getValue().textValue())
                    .put("type", "default")
                    .put("enabled", true));
    compararJson("postman/TicketYa-Entrega1.postman_collection.json", coleccion);
    compararJson("postman/local.postman_environment.json", entorno);
  }

  record Indice(String tabla, String nombre) {}

  record Columna(String tabla, String nombre) {
    static Columna de(JsonNode c) {
      return new Columna(c.required("table").asText(), c.required("column").asText());
    }
  }

  record Modelo(String diagrama, String restricciones) {}

  static List<JsonNode> delEsquema(JsonNode meta, String campo, String servicio) {
    return meta.required(campo)
        .valueStream()
        .filter(n -> n.required("schema").asText().equals("ticketya_e1_" + servicio))
        .toList();
  }

  static Modelo modelo(JsonNode meta, String servicio) {
    var columnas = delEsquema(meta, "columns", servicio);
    var fks = delEsquema(meta, "foreign_keys", servicio);
    var indices =
        delEsquema(meta, "unique_indexes", servicio).stream()
            .collect(
                Collectors.groupingBy(
                    i -> new Indice(i.required("table").asText(), i.required("index").asText()),
                    LinkedHashMap::new,
                    Collectors.toList()));
    Set<Columna> unicas = new HashSet<>(), externas = new HashSet<>(), opcionales = new HashSet<>();
    var compuestas = new ArrayList<String>();
    for (var entry : indices.entrySet()) {
      var id = entry.getKey();
      var campos =
          entry.getValue().stream()
              .sorted(Comparator.comparingInt(n -> n.required("position").asInt()))
              .map(n -> n.required("column").asText())
              .toList();
      if (campos.size() > 1)
        compuestas.add(
            "- `"
                + id.tabla
                + "`: `"
                + String.join(", ", campos)
                + "` ("
                + (id.nombre.equals("PRIMARY") ? "PK" : "UK")
                + ").");
      else if (!id.nombre.equals("PRIMARY")) unicas.add(new Columna(id.tabla, campos.getFirst()));
    }
    fks.forEach(f -> externas.add(Columna.de(f)));
    var tablas =
        columnas.stream()
            .collect(
                Collectors.groupingBy(
                    c -> c.required("table").asText(), TreeMap::new, Collectors.toList()));
    var lines = new ArrayList<String>();
    lines.add("erDiagram");
    tablas.forEach(
        (tabla, cols) -> {
          lines.add("    " + tabla + " {");
          for (var col : cols) {
            var clave = Columna.de(col);
            var keys = new ArrayList<String>();
            if (col.required("key").asText().equals("PRI")) keys.add("PK");
            if (externas.contains(clave)) keys.add("FK");
            if (unicas.contains(clave)) keys.add("UK");
            boolean nullable = col.required("nullable").asText().equals("YES");
            if (nullable) opcionales.add(clave);
            String tipo = col.required("type").asText().split("\\(")[0].replace(' ', '_');
            lines.add(
                "        "
                    + tipo
                    + " "
                    + clave.nombre
                    + (keys.isEmpty() ? "" : " " + String.join(",", keys))
                    + " \""
                    + (nullable ? "nullable" : "required")
                    + "\"");
          }
          lines.add("    }");
        });
    for (var f : fks) {
      var c = Columna.de(f);
      lines.add(
          "    "
              + f.required("target_table").asText()
              + " "
              + (opcionales.contains(c) ? "|o" : "||")
              + "--"
              + (unicas.contains(c) ? "o|" : "o{")
              + " "
              + c.tabla
              + " : \""
              + c.nombre
              + "\"");
    }
    if (compuestas.isEmpty()) compuestas.add("- No hay índices únicos compuestos en el snapshot.");
    return new Modelo(String.join("\n", lines) + "\n", String.join("\n", compuestas));
  }

  @Test
  void generarDiagramasYPaginas() throws Exception {
    var meta = leer(DOC.resolve("modelos/esquema-referencia.json"));
    var diagramas = new HashMap<String, String>();
    for (var servicio : SERVICIOS) {
      var modelo = modelo(meta, servicio);
      String nombre = "er-" + servicio + ".mmd";
      compararTexto("diagramas/" + nombre, modelo.diagrama);
      diagramas.put(nombre, modelo.diagrama);
    }
    var bloques =
        Pattern.compile("(?s)```mermaid\n.*?```(\\s*\\[Fuente Mermaid]\\(diagramas/([^)]*)\\)\\.)");
    for (var pagina : List.of("arquitectura.md", "secuencias.md", "modelo-datos.md")) {
      var source = texto(DOC.resolve(pagina));
      var m = bloques.matcher(source);
      var result = new StringBuilder();
      int cuenta = 0;
      while (m.find()) {
        String nombre = m.group(2);
        String figura = diagramas.get(nombre);
        if (figura == null) {
          figura = texto(DOC.resolve("diagramas/" + nombre));
          guardar("diagramas/" + nombre, figura);
        }
        m.appendReplacement(
            result, Matcher.quoteReplacement("```mermaid\n" + figura + "```" + m.group(1)));
        cuenta++;
      }
      assertEquals(
          Map.of("arquitectura.md", 1, "secuencias.md", 6, "modelo-datos.md", 4)
              .get(pagina)
              .intValue(),
          cuenta);
      m.appendTail(result);
      String generado = result.toString();
      if (pagina.equals("modelo-datos.md")) {
        for (var servicio : SERVICIOS) {
          String patron =
              "(?s)(diagramas/er-"
                  + servicio
                  + "\\.mmd\\)\\.\n\nRestricciones únicas compuestas[^\n]*\n\n)(?:- [^\n]*\n)+";
          generado =
              Pattern.compile(patron)
                  .matcher(generado)
                  .replaceFirst(
                      mr ->
                          Matcher.quoteReplacement(
                              mr.group(1) + modelo(meta, servicio).restricciones + "\n"));
        }
      }
      compararTexto(pagina, generado);
    }
  }
}
