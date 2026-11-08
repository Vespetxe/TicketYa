package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.client.ReservaClientSimulado;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiIntegracionTest {
    @LocalServerPort int port;
    @MockitoSpyBean ReservaClientSimulado reservas;
    @Autowired ObjectMapper mapper;
    @Autowired ar.edu.unicen.grupo8.comprasservice.repository.PagoRepository pagos;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    private final HttpClient http = HttpClient.newHttpClient();
    private long reserva;
    private long cliente;

    @BeforeEach void preparar() {
        reserva = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1000000000L) + 10000;
        cliente = reserva;
        reservas.registrarReserva(new ReservaDTO(reserva, cliente, 20L, "TEMPORAL", LocalDateTime.now(Clock.systemUTC()).plusMinutes(15),
                List.of(new DetalleEmisionDTO(1L, null, 2))));
    }
    private HttpResponse<String> solicitar(String metodo, String ruta, String cuerpo) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + ruta));
        if (cuerpo == null) builder.GET();
        else builder.header("Content-Type", "application/json").method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private String compraJson(String detalle) {
        return "{\"clienteId\":" + cliente + ",\"eventoId\":20,\"reservaId\":" + reserva + ",\"detalles\":" + detalle + "}";
    }
    private long crear() throws Exception {
        var respuesta = solicitar("POST", "/compras", compraJson("[{\"sectorId\":1,\"cantidad\":2,\"precioUnitario\":1}]"));
        assertEquals(201, respuesta.statusCode(), respuesta.body());
        JsonNode json = mapper.readTree(respuesta.body());
        assertEquals(30000, json.get("importeTotal").decimalValue().intValueExact());
        assertEquals(15000, json.get("detalles").get(0).get("precioUnitario").decimalValue().intValueExact());
        assertFalse(json.get("detalles").get(0).has("compra")); assertFalse(json.has("ejecutor"));
        return json.get("id").asLong();
    }
    private String pagoJson(long compra, String clave) { return "{\"compraId\":" + compra + ",\"claveIdempotencia\":\"" + clave + "\",\"medioPago\":\"TARJETA_DEBITO\"}"; }
    private void error(HttpResponse<String> r, int estado) throws Exception {
        assertEquals(estado, r.statusCode(), r.body()); JsonNode json = mapper.readTree(r.body());
        assertEquals(estado, json.get("status").asInt()); assertTrue(json.get("message").isTextual());
    }

    @Test void circuitoHttpCompletoSerializaDtoYRepiteSinDuplicar() throws Exception {
        long id = crear(); String solicitud = pagoJson(id, "http-" + reserva);
        var respuesta = solicitar("POST", "/pagos", solicitud); assertEquals(201, respuesta.statusCode(), respuesta.body());
        JsonNode p = mapper.readTree(respuesta.body()); assertEquals(id, p.get("compraId").asLong());
        assertEquals("APROBADO", p.get("estado").asText()); assertFalse(p.has("compra"));
        var repetida = solicitar("POST", "/pagos", solicitud); assertEquals(200, repetida.statusCode());
        assertEquals(p.get("id"), mapper.readTree(repetida.body()).get("id"));
        var compra = solicitar("GET", "/compras/" + id, null); assertEquals(200, compra.statusCode());
        JsonNode c = mapper.readTree(compra.body()); assertEquals("COMPLETADA", c.get("estado").asText()); assertEquals(2, c.get("entradasIds").size());
        assertEquals(200, solicitar("GET", "/compras/" + id + "/estado", null).statusCode());
        assertEquals(200, solicitar("GET", "/compras/" + id + "/detalle", null).statusCode());
        assertEquals(200, solicitar("GET", "/compras?clienteId=" + cliente, null).statusCode());
        assertEquals(200, solicitar("GET", "/pagos/" + p.get("id").asLong(), null).statusCode());
        assertEquals(200, solicitar("GET", "/pagos?compraId=" + id, null).statusCode());
        error(solicitar("POST", "/pagos", pagoJson(id, "otra-" + reserva)), 409);
        error(solicitar("POST", "/compras/" + id + "/cancelar", "{}"), 409);
    }
    @Test void validacionesYErroresTienenFormatoUniforme() throws Exception {
        error(solicitar("POST", "/compras", compraJson("[null]")), 400);
        error(solicitar("POST", "/compras", compraJson("[{\"sectorId\":1,\"cantidad\":0}]")), 400);
        error(solicitar("POST", "/compras", compraJson("[{\"sectorId\":2,\"cantidad\":2}]")), 400);
        error(solicitar("POST", "/compras", "{"), 400);
        error(solicitar("GET", "/compras?clienteId=-1", null), 400);
        error(solicitar("GET", "/compras", null), 400);
        error(solicitar("GET", "/compras/no-numero", null), 400);
        error(solicitar("GET", "/compras/9223372036854775807", null), 404);
        error(solicitar("GET", "/pagos/9223372036854775807", null), 404);
        error(solicitar("POST", "/pagos", pagoJson(Long.MAX_VALUE, "inexistente")), 404);
        error(solicitar("POST", "/pagos", pagoJson(1, "x".repeat(101))), 400);
    }
    @Test void creacionRepetidaDeReservaDevuelveConflicto() throws Exception {
        crear(); error(solicitar("POST", "/compras", compraJson("[{\"sectorId\":1,\"cantidad\":2}]")), 409);
    }
    @Test void erroresDeProtocoloConservanElMismoFormato() throws Exception {
        error(solicitar("DELETE", "/compras", "{}"), 405);
        error(solicitar("GET", "/ruta-inexistente", null), 404);
        var solicitud = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/compras"))
                .header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString("texto")).build();
        error(http.send(solicitud, HttpResponse.BodyHandlers.ofString()), 415);
    }
    @Test void formatoNoAceptableDevuelve406ConErrorJson() throws Exception {
        var solicitud = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/compras?clienteId=" + cliente))
                .header("Accept", "application/xml").GET().build();
        var respuesta = http.send(solicitud, HttpResponse.BodyHandlers.ofString());
        error(respuesta, 406);
        assertTrue(respuesta.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }
    @Test void errorInternoDevuelve500UniformeSinExponerDetalles() throws Exception {
        doThrow(new RuntimeException("detalle-interno-que-no-debe-exponerse")).when(reservas).reservaValida(reserva, cliente, 20L);
        var respuesta = solicitar("POST", "/compras", compraJson("[{\"sectorId\":1,\"cantidad\":2}]"));
        error(respuesta, 500);
        assertFalse(respuesta.body().contains("detalle-interno"));
        assertFalse(respuesta.body().contains("RuntimeException"));
    }
    @Test void cancelaCompraPendienteDeFormaIdempotenteEImpidePago() throws Exception {
        long id = crear();
        var respuesta = solicitar("POST", "/compras/" + id + "/cancelar", "{}");
        assertEquals(200, respuesta.statusCode(), respuesta.body());
        JsonNode c = mapper.readTree(respuesta.body());
        assertEquals("CANCELADA", c.get("estado").asText()); assertEquals("FINALIZADA", c.get("etapa").asText());
        assertFalse(c.get("detalles").get(0).has("compra"));
        assertEquals("LIBERADA", reservas.consultarReserva(reserva).estado());
        assertEquals(200, solicitar("POST", "/compras/" + id + "/cancelar", "{}").statusCode());
        verify(reservas, times(1)).liberarReserva(reserva);
        error(solicitar("POST", "/pagos", pagoJson(id, "cancelada-" + reserva)), 409);
        error(solicitar("POST", "/compras/0/cancelar", "{}"), 400);
        error(solicitar("POST", "/compras/9223372036854775807/cancelar", "{}"), 404);
    }
    @Test void swaggerYOpenApiPublicanContratosReales() throws Exception {
        var respuesta = solicitar("GET", "/v3/api-docs", null); assertEquals(200, respuesta.statusCode(), respuesta.body());
        JsonNode documento = mapper.readTree(respuesta.body());
        assertTrue(documento.get("paths").has("/compras")); assertTrue(documento.get("paths").has("/pagos"));
        assertTrue(documento.get("paths").has("/compras/{id}/cancelar"));
        assertTrue(documento.get("components").get("schemas").has("CompraDTO"));
        assertTrue(documento.get("components").get("schemas").has("PagoDTO"));
        var swagger = solicitar("GET", "/swagger-ui/index.html", null);
        assertEquals(200, swagger.statusCode()); assertTrue(swagger.body().contains("swagger-ui"));
    }

    @Test void registraYConsultaTodosLosMediosSinDuplicarPagos() throws Exception {
        for (var medio : ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago.values()) {
            preparar(); long compra = crear(); String clave = "medio-" + reserva;
            String cuerpo = mapper.writeValueAsString(Map.of("compraId", compra, "claveIdempotencia", clave, "medioPago", medio));
            var r = solicitar("POST", "/pagos", cuerpo); assertEquals(201, r.statusCode(), r.body());
            var p = mapper.readTree(r.body()); assertEquals(medio.name(), p.get("medioPago").asText());
            var repetida = solicitar("POST", "/pagos", cuerpo); assertEquals(200, repetida.statusCode(), repetida.body());
            assertEquals(p.get("id"), mapper.readTree(repetida.body()).get("id"));
            var consulta = solicitar("GET", "/pagos/" + p.get("id").asLong(), null);
            assertEquals(medio.name(), mapper.readTree(consulta.body()).get("medioPago").asText());
            assertEquals(medio, pagos.findById(p.get("id").asLong()).orElseThrow().getMedioPago());
            var lista = solicitar("GET", "/pagos?compraId=" + compra, null);
            assertEquals(medio.name(), mapper.readTree(lista.body()).get(0).get("medioPago").asText());
        }
    }
    @Test void medioNuevoEsObligatorioYNoAdmiteValoresDesconocidosNiOrdinales() throws Exception {
        long compra = crear();
        String base = "{\"compraId\":" + compra + ",\"claveIdempotencia\":\"validacion-medio\"";
        error(solicitar("POST", "/pagos", base + "}"), 400);
        for (String valor : List.of("null", "\"\"", "\"EFECTIVO\"", "0", "{}"))
            error(solicitar("POST", "/pagos", base + ",\"medioPago\":" + valor + "}"), 400);
        assertTrue(pagos.findByCompraId(compra).isEmpty());
    }
    @Test void reintentoConOtroMedioOmitidoNoCambiaLaOperacionOriginal() throws Exception {
        long compra = crear(); String clave = "identidad-" + reserva;
        assertEquals(201, solicitar("POST", "/pagos", pagoJson(compra, clave)).statusCode());
        String otro = mapper.writeValueAsString(Map.of("compraId", compra, "claveIdempotencia", clave, "medioPago", "TRANSFERENCIA"));
        error(solicitar("POST", "/pagos", otro), 409);
        error(solicitar("POST", "/pagos", mapper.writeValueAsString(Map.of("compraId", compra, "claveIdempotencia", clave))), 400);
        var guardados = pagos.findByCompraId(compra); assertEquals(1, guardados.size());
        assertEquals(ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago.TARJETA_DEBITO, guardados.get(0).getMedioPago());
    }
    @Test void historicoSinMedioSeConsultaYRepiteSinInventarInformacion() throws Exception {
        long compra = crear(); String clave = "historico-" + reserva;
        var creado = solicitar("POST", "/pagos", pagoJson(compra, clave)); assertEquals(201, creado.statusCode());
        long id = mapper.readTree(creado.body()).get("id").asLong();
        new org.springframework.transaction.support.TransactionTemplate(manager).executeWithoutResult(s -> pagos.findById(id).orElseThrow().setMedioPago(null));
        var consulta = solicitar("GET", "/pagos/" + id, null); assertTrue(mapper.readTree(consulta.body()).get("medioPago").isNull());
        var repetida = solicitar("POST", "/pagos", mapper.writeValueAsString(Map.of("compraId", compra, "claveIdempotencia", clave)));
        assertEquals(200, repetida.statusCode(), repetida.body()); assertTrue(mapper.readTree(repetida.body()).get("medioPago").isNull());
        error(solicitar("POST", "/pagos", pagoJson(compra, clave)), 409);
        assertEquals(1, pagos.findByCompraId(compra).size()); assertNull(pagos.findById(id).orElseThrow().getMedioPago());
    }
    @Test void openApiDescribeMedioCondicionalEnSolicitudYDatoHistoricoEnRespuesta() throws Exception {
        var r = solicitar("GET", "/v3/api-docs", null); assertEquals(200, r.statusCode());
        var schemas = mapper.readTree(r.body()).at("/components/schemas");
        var solicitud = schemas.at("/ProcesarPagoDTO/properties/medioPago");
        assertFalse(solicitud.isMissingNode()); assertTrue(solicitud.get("description").asText().contains("Obligatorio"));
        assertFalse(schemas.at("/PagoDTO/properties/medioPago").isMissingNode());
        for (var medio : ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago.values()) assertTrue(schemas.toString().contains(medio.name()));
    }
}
