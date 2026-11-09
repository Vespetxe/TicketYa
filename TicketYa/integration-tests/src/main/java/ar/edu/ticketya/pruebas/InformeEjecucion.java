package ar.edu.ticketya.pruebas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

public final class InformeEjecucion {
  private static final ObjectMapper JSON = new ObjectMapper();

  private static void exigir(boolean ok, String mensaje) {
    if (!ok) throw new IllegalStateException(mensaje);
  }

  static String hash(Path path) throws Exception {
    var digest = MessageDigest.getInstance("SHA-256");
    try (var in = Files.newInputStream(path)) {
      byte[] buffer = new byte[65536];
      int leidos;
      while ((leidos = in.read(buffer)) != -1) digest.update(buffer, 0, leidos);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  static ObjectNode suites(Path dir) throws Exception {
    var xml = DocumentBuilderFactory.newInstance();
    xml.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var parser = xml.newDocumentBuilder();
    var result = JSON.createObjectNode();
    var suites = result.putArray("suites");
    int total = 0;
    try (var list = Files.newDirectoryStream(dir, "TEST-*.xml")) {
      for (Path p : list) {
        var suite = parser.parse(p.toFile()).getDocumentElement();
        exigir(suite.getTagName().equals("testsuite"), "XML no reconocido: " + p);
        int tests = Integer.parseInt(suite.getAttribute("tests"));
        var item = suites.addObject().put("nombre", suite.getAttribute("name")).put("tests", tests);
        for (String key : List.of("failures", "errors", "skipped")) {
          int valor = Integer.parseInt(suite.getAttribute(key));
          item.put(key, valor);
          exigir(valor == 0, p + " contiene " + key);
        }
        var casos = item.putArray("casos");
        var nodes = suite.getElementsByTagName("testcase");
        for (int i = 0; i < nodes.getLength(); i++)
          casos.add(((Element) nodes.item(i)).getAttribute("name"));
        exigir(casos.size() == tests, "Número de casos inconsistente: " + p);
        total += tests;
      }
    }
    exigir(total > 0, "Sin casos en " + dir);
    return result.put("tests", total);
  }

  static void comprobarTotales(Path archivo, List<Integer> esperados) throws Exception {
    String log = Files.readString(archivo);
    var totales = Pattern.compile("(?m)^\\[INFO] Tests run: (\\d+), Failures: 0, Errors: 0, Skipped: 0\\s*$")
        .matcher(log).results().map(m -> Integer.parseInt(m.group(1))).toList();
    exigir(log.contains("[INFO] BUILD SUCCESS") && totales.equals(esperados),
        "Los XML no concuerdan con el resultado Maven de " + archivo);
  }

  public static void main(String[] args) throws Exception {
    String run = System.getenv("TICKETYA_RUN_ID");
    exigir(run != null && run.matches("[a-f0-9]{32}"), "Falta el identificador de corrida");
    Path module = Path.of(System.getProperty("ticketya.modulo", ".")).toAbsolutePath().normalize();
    Path dir = module.resolve("target/runs/" + run);
    var report =
        JSON.createObjectNode().put("corrida", run).put("fecha_utc", Instant.now().toString());
    var servicios = report.putObject("servicios");
    report.set("origen", JSON.readTree(dir.resolve("origen.json").toFile()));
    int total = 0;
    for (String servicio :
        List.of(
            "EventoCatalogo_Service",
            "inventarioReserva_service",
            "comprapago_service",
            "entradas_service",
            "api-gateway")) {
      var resultado = suites(dir.resolve("servicios/" + servicio));
      comprobarTotales(dir.resolve("servicios/" + servicio + ".log"), List.of(resultado.required("tests").intValue()));
      servicios.set(servicio, resultado);
      total += resultado.required("tests").intValue();
    }
    report.put("tests_servicios", total);
    report.set("locales", suites(dir.resolve("surefire-reports")));
    report.set("acreditacion_parcial", suites(dir.resolve("previo/surefire-reports")));
    var http = suites(dir.resolve("failsafe-reports"));
    exigir(
        http.required("tests").intValue() == 14, "Se esperan los 14 escenarios HTTP de Entrega 1");
    report.set("http", http);
    comprobarTotales(dir.resolve("verify.log"), List.of(report.at("/locales/tests").intValue(),http.required("tests").intValue()));
    comprobarTotales(dir.resolve("acreditacion-parcial.log"), List.of(report.at("/acreditacion_parcial/tests").intValue()));
    var procesos = JSON.readTree(dir.resolve("processes.json").toFile());
    Set<Integer> puertos = new HashSet<>();
    Set<Long> pids = new HashSet<>();
    var inventarios = new ArrayList<String>();
    for (var proceso : procesos) {
      Path jar = Path.of(proceso.required("artifact").textValue()).normalize();
      exigir(
          jar.startsWith(module.getParent())
              && jar.getParent().getParent().getFileName().toString().equals(run),
          "JAR ajeno a la corrida");
      String digest = hash(jar);
      exigir(
          digest.equalsIgnoreCase(proceso.required("sha256").asText()), "Hash de JAR incorrecto");
      puertos.add(proceso.required("port").intValue());
      pids.add(proceso.required("pid").longValue());
      if (proceso.required("service").asText().equals("inventario")) inventarios.add(digest);
      ((ObjectNode) proceso).put("artifact", module.getParent().relativize(jar).toString());
    }
    exigir(
        pids.size() == 6
            && puertos.equals(Set.of(28080, 28081, 28082, 28083, 28084, 28085))
            && inventarios.size() == 2
            && inventarios.get(0).equals(inventarios.get(1)),
        "Topología integral incompleta");
    report.set("procesos", procesos);
    JsonNode postman = JSON.readTree(dir.resolve("postman.json").toFile());
    Path coleccion =
        module
            .getParent()
            .getParent()
            .resolve("docs/entrega1/postman/TicketYa-Entrega1.postman_collection.json");
    int solicitudes =
        JSON.readTree(coleccion.toFile())
            .required("item")
            .valueStream()
            .mapToInt(g -> g.required("item").size())
            .sum();
    exigir(
        hash(coleccion).equals(postman.required("collection_sha256").asText())
            && postman.required("requests").intValue() == solicitudes
            && postman.required("results").size() == solicitudes
            && postman.required("checks").intValue() > 0
            && postman.required("failures").intValue() == 0,
        "Informe Postman incompleto");
    report.set("postman", postman);
    Path salida = dir.resolve("resumen.json");
    Files.writeString(
        salida,
        JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report) + "\n",
        StandardOpenOption.CREATE_NEW);
    System.out.println(salida);
  }
}
