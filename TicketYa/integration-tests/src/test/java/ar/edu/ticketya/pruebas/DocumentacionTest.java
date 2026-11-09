package ar.edu.ticketya.pruebas;

import static ar.edu.ticketya.pruebas.GenerarDocumentacionTest.*;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import org.junit.jupiter.api.Test;

class DocumentacionTest {
  static final Map<String, String> PROYECTOS =
      Map.of(
          "eventos",
          "EventoCatalogo_Service",
          "inventario",
          "inventarioReserva_service",
          "compras",
          "comprapago_service",
          "entradas",
          "entradas_service");
  static final Set<String> METODOS = Set.of("get", "post", "patch", "put", "delete");

  static String normalizar(String path) {
    return path.replaceAll("\\{\\{[^}]+}}|\\{[^}]+}", "{}");
  }

  static List<String> capturas(String regex, String texto) {
    return Pattern.compile(regex).matcher(texto).results().map(m -> m.group(1)).toList();
  }

  static void referencias(JsonNode campo, JsonNode doc) {
    if (campo.has("$ref")) {
      String ref = campo.required("$ref").asText();
      assertTrue(ref.startsWith("#/"), ref);
      assertFalse(doc.at(ref.substring(1)).isMissingNode(), ref);
    }
    campo.forEach(hijo -> referencias(hijo, doc));
  }

  static boolean tipo(JsonNode v, String tipo) {
    return switch (tipo) {
      case "string" -> v.isTextual();
      case "object" -> v.isObject();
      case "integer" -> v.isIntegralNumber();
      case "boolean" -> v.isBoolean();
      case "number" -> v.isNumber();
      case "array" -> v.isArray();
      case "null" -> v.isNull();
      default -> false;
    };
  }

  static boolean coincide(JsonNode v, JsonNode s, JsonNode doc) {
    if (s.has("$ref")) return coincide(v, doc.at(s.get("$ref").asText().substring(1)), doc);
    if (s.has("anyOf")) return s.get("anyOf").valueStream().anyMatch(o -> coincide(v, o, doc));
    if (s.has("allOf")) return s.get("allOf").valueStream().allMatch(o -> coincide(v, o, doc));
    if (v.isNull() && s.path("nullable").asBoolean()) return true;
    if (s.has("enum") && s.get("enum").valueStream().noneMatch(v::equals)) return false;
    var tipos = s.path("type");
    if (tipos.isArray() && tipos.valueStream().noneMatch(t -> tipo(v, t.asText()))) return false;
    if (tipos.isTextual() && !tipo(v, tipos.asText())) return false;
    if (v.isObject()) {
      if (s.path("required").valueStream().anyMatch(k -> !v.has(k.asText()))) return false;
      for (var prop : v.properties()) {
        var spec = s.path("properties").path(prop.getKey());
        if (spec.isMissingNode() && !s.path("additionalProperties").asBoolean(true)) return false;
        if (!coincide(prop.getValue(), spec, doc)) return false;
      }
    }
    if (v.isArray())
      return v.size() >= s.path("minItems").asInt(0)
          && v.valueStream().allMatch(i -> coincide(i, s.path("items"), doc));
    if (v.isTextual())
      return v.asText().length() >= s.path("minLength").asInt(0)
          && v.asText().length() <= s.path("maxLength").asInt(Integer.MAX_VALUE);
    if (v.isNumber())
      return v.asDouble() >= s.path("minimum").asDouble(Double.NEGATIVE_INFINITY)
          && v.asDouble() <= s.path("maximum").asDouble(Double.POSITIVE_INFINITY);
    return true;
  }

  static Set<String> controladores(String proyecto) throws Exception {
    var rutas = new TreeSet<String>();
    try (var files = Files.walk(ROOT.resolve("TicketYa/" + proyecto + "/src/main/java"))) {
      for (var p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
        String source = texto(p);
        if (!source.contains("@RestController")
            || source.contains("@Profile(\"pruebas-integrales\")")) continue;
        var base =
            capturas(
                "@RequestMapping\\(\\s*\"([^\"]*)\"",
                source.substring(0, source.indexOf("public class ")));
        var mapping =
            Pattern.compile("@(Get|Post|Put|Patch|Delete)Mapping\\b(?:\\s*\\(([^)]*)\\))?")
                .matcher(source);
        while (mapping.find()) {
          var sufijo = capturas("\"([^\"]*)\"", Objects.toString(mapping.group(2), ""));
          String ruta =
              (base.isEmpty() ? "" : base.getFirst()) + (sufijo.isEmpty() ? "" : sufijo.getFirst());
          rutas.add(mapping.group(1).toLowerCase(Locale.ROOT) + " " + normalizar(ruta));
        }
      }
    }
    return rutas;
  }

  @Test
  void contratosYControladores() throws Exception {
    for (String servicio : List.of("eventos", "inventario", "compras", "entradas", "gateway")) {
      var doc = leer(DOC.resolve("openapi/" + servicio + ".json"));
      assertTrue(doc.required("openapi").asText().startsWith("3.1"));
      referencias(doc, doc);
      var documentadas = new TreeSet<String>();
      for (var ruta : doc.required("paths").properties()) {
        String path = ruta.getKey();
        if (servicio.equals("gateway"))
          assertFalse(
              path.startsWith("/internal/")
                  || path.endsWith("/confirmar")
                  || path.endsWith("/liberar"),
              path);
        for (var op : ruta.getValue().properties()) {
          if (!METODOS.contains(op.getKey())) continue;
          var operacion = op.getValue();
          documentadas.add(op.getKey() + " " + normalizar(path));
          for (var nombre : capturas("\\{([^}]+)}", path))
            assertTrue(
                operacion
                    .path("parameters")
                    .valueStream()
                    .anyMatch(
                        p ->
                            p.path("in").asText().equals("path")
                                && p.path("name").asText().equals(nombre)
                                && p.path("required").asBoolean()),
                path + " " + nombre);
          var respuestas = operacion.required("responses");
          assertTrue(
              respuestas.properties().stream().anyMatch(r -> r.getKey().startsWith("2")), path);
          var cuerpos =
              Stream.concat(Stream.of(operacion.path("requestBody")), respuestas.valueStream());
          cuerpos
              .flatMap(c -> c.path("content").valueStream())
              .filter(c -> c.has("schema") && c.has("example"))
              .forEach(
                  c ->
                      assertTrue(
                          coincide(c.get("example"), c.get("schema"), doc),
                          servicio + " " + path + " " + op.getKey() + " " + c.get("example")));
        }
      }
      if (PROYECTOS.containsKey(servicio))
        assertEquals(controladores(PROYECTOS.get(servicio)), documentadas, servicio);
      if (servicio.equals("eventos"))
        for (String suffix : List.of("status", "suspend"))
          assertTrue(
              doc.get("paths")
                  .required("/api/v1/events/{eventId}/" + suffix)
                  .at("/patch/responses")
                  .has("404"));
      if (servicio.equals("inventario"))
        assertTrue(
            doc.get("paths").required("/ocupaciones/liberar").at("/post/responses").has("400"));
    }
    assertEquals(
        texto(DOC.resolve("openapi/entradas.json")),
        texto(ROOT.resolve("TicketYa/entradas_service/docs/openapi.yaml")));
  }

  @Test
  void coleccionContraGatewayYSintaxisJavaScript() throws Exception {
    var paths = leer(DOC.resolve("openapi/gateway.json")).required("paths");
    var entorno = leer(DOC.resolve("postman/local.postman_environment.json")).required("values");
    var variables =
        entorno.valueStream().map(v -> v.required("key").asText()).collect(Collectors.toSet());
    var collection = leer(DOC.resolve("postman/TicketYa-Entrega1.postman_collection.json"));
    var scripts = new StringBuilder();
    for (var grupo : collection.required("item"))
      for (var item : grupo.required("item")) {
        var req = item.required("request");
        String nombre = item.required("name").asText(), url = req.required("url").asText();
        for (var key : capturas("\\{\\{([^}]+)}}", item.toString()))
          assertTrue(key.startsWith("$") || variables.contains(key), nombre + " " + key);
        assertTrue(url.startsWith("{{base_url}}/"), url);
        var partes = url.substring("{{base_url}}".length()).split("\\?", 2);
        var metodo = req.required("method").asText().toLowerCase(Locale.ROOT);
        var op =
            paths.properties().stream()
                .filter(
                    p ->
                        normalizar(p.getKey()).equals(normalizar(partes[0]))
                            && p.getValue().has(metodo))
                .findFirst()
                .orElseThrow(() -> new AssertionError(url))
                .getValue()
                .required(metodo);
        var query =
            partes.length == 2
                ? Arrays.stream(partes[1].split("&"))
                    .map(q -> q.split("=", 2)[0])
                    .collect(Collectors.toSet())
                : Set.of();
        var headers =
            req.path("header")
                .valueStream()
                .filter(h -> !h.path("disabled").asBoolean())
                .map(h -> h.required("key").asText().toLowerCase(Locale.ROOT))
                .toList();
        for (var p : op.path("parameters"))
          if (p.path("required").asBoolean()) {
            String key = p.required("name").asText();
            if (p.path("in").asText().equals("query"))
              assertTrue(query.contains(key), nombre + " " + key);
            if (p.path("in").asText().equals("header"))
              assertTrue(headers.contains(key.toLowerCase(Locale.ROOT)), nombre + " " + key);
          }
        if (req.has("body"))
          JSON.readTree(req.get("body").required("raw").asText().replaceAll("\\{\\{[^}]+}}", "1"));
        for (var event : item.path("event")) {
          String js = String.join("\n", lineas(event.required("script").required("exec")));
          for (String codigos : capturas("pm.expect\\((\\[[0-9, ]+])\\)", js))
            for (var code : JSON.readTree(codigos))
              assertTrue(op.required("responses").has(code.asText()), nombre + " " + code);
          scripts.append("(function(pm){\n").append(js).append("\n});\n");
        }
      }
    Path archivo = OUT.resolve("postman-syntax.cjs");
    Files.createDirectories(OUT);
    Files.writeString(archivo, scripts, StandardCharsets.UTF_8);
    var proceso =
        new ProcessBuilder("node", "--check", archivo.toString()).redirectErrorStream(true).start();
    var diagnostico = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(0, proceso.waitFor(), diagnostico);
  }

  @Test
  void enlacesYMarkdown() throws Exception {
    Set<Path> excluidos =
        Stream.of(
                "docs/entrega1/cierre-astra.md",
                "docs/entrega1/preparacion-git.md",
                "docs/entrega1/manifiesto-git.json",
                "docs/entrega1/revision-indice.json",
                "docs/entrega1/credenciales-configuracion.json",
                "docs/entrega1/tools/preparar_manifiesto_git.py",
                "docs/entrega1/tools/verificar_indice.py",
                "TicketYa/HELP.md",
                "TicketYa/inventarioReserva_service/HELP.md")
            .map(ROOT::resolve)
            .collect(Collectors.toSet());
    List<Path> paginas = new ArrayList<>();
    for (String ruta :
        List.of(
            "docs/entrega1",
            "TicketYa/comprapago_service/docs",
            "TicketYa/comprapago_service/openspec"))
      try (var files = Files.walk(ROOT.resolve(ruta))) {
        paginas.addAll(
            files.filter(f -> f.toString().endsWith(".md") && !excluidos.contains(f)).toList());
      }
    for (String ruta :
        List.of(
            "README.md",
            "TicketYa/inventarioReserva_service/README.md",
            "TicketYa/entradas_service/docs/compras-order-contract.md"))
      paginas.add(ROOT.resolve(ruta));
    for (var pagina : paginas) {
      String md = texto(pagina);
      assertFalse(
          Pattern.compile("C:[/\\\\]Users[/\\\\]", Pattern.CASE_INSENSITIVE).matcher(md).find(),
          pagina.toString());
      assertEquals(0, (md.split("```", -1).length - 1) % 2, pagina.toString());
      for (var target : capturas("\\[[^\\]]*]\\(([^)]+)\\)", md)) {
        if (target.contains("://") || target.startsWith("#")) continue;
        var path = pagina.getParent().resolve(target.split("#", 2)[0]).normalize();
        assertTrue(!excluidos.contains(path) && Files.exists(path), pagina + " -> " + target);
      }
    }
  }
}
