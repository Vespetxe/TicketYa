package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import ar.edu.unicen.grupo8.comprasservice.exception.*;
import ar.edu.unicen.grupo8.comprasservice.repository.*;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DevolucionHttpIntegracionTest {
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    private final java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    @Autowired DevolucionService servicio;
    @Autowired ReintegroService flujo;
    @Autowired ReintegroRegistroService registro;
    @Autowired CompraService compras;
    @Autowired PagoService pagos;
    @Autowired CompraRepository compraRepository;
    @Autowired PagoRepository pagoRepository;
    @Autowired DevolucionRepository devoluciones;
    @Autowired DetalleDevolucionRepository detalles;
    @Autowired ReintegroRepository reintegros;
    @Autowired ControlLimiteEntradasRepository limites;
    @Autowired PlatformTransactionManager manager;
    @Autowired AlmacenSimulado almacen;
    @Autowired ObjectMapper mapper;
    @MockitoBean Clock clock;
    @MockitoSpyBean EventoClientSimulado eventos;
    @MockitoSpyBean EntradaClientSimulado entradas;
    @MockitoSpyBean ReservaClientSimulado reservas;
    @MockitoSpyBean PasarelaPagoSimuladaService pasarela;

    @BeforeEach void preparar() {
        reset(eventos, entradas, reservas, pasarela);
        when(clock.instant()).thenReturn(AHORA); when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        limpiar(); almacen.limpiar("evento:"); pasarela.reiniciar(); entradas.reiniciarEntradas(); reservas.reiniciarReservas();
    }
    @AfterEach void limpiar() {
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            reintegros.deleteAll(); detalles.deleteAll(); devoluciones.deleteAll();
            pagoRepository.deleteAll(); compraRepository.deleteAll(); limites.deleteAll();
        });
        almacen.limpiar("evento:"); pasarela.reiniciar(); entradas.reiniciarEntradas(); reservas.reiniciarReservas();
    }
    private Compra crear(boolean pagar) {
        DetalleCompraDTO detalle = new DetalleCompraDTO(); detalle.setSectorId(1L); detalle.setCantidad(2);
        CrearCompraDTO dto = new CrearCompraDTO(); dto.setClienteId(10L); dto.setEventoId(20L); dto.setReservaId(30L); dto.setDetalles(List.of(detalle));
        Compra c = compras.crearCompra(dto);
        if (pagar) pagos.procesarPago(c.getId(), "pago-devolucion", MedioPago.TARJETA_DEBITO);
        c = compras.obtenerCompraPorId(c.getId());
        clearInvocations(eventos, entradas, reservas, pasarela);
        return c;
    }
    private java.net.http.HttpResponse<String> http(String metodo, String ruta, Object dto) throws Exception {
        var b = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + port + ruta));
        if (dto == null) b.GET(); else b.header("Content-Type", "application/json").method(metodo,
                java.net.http.HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(dto)));
        return http.send(b.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
    }
    private void error(java.net.http.HttpResponse<String> r, int status) {
        assertEquals(status, r.statusCode(), r.body()); var json = mapper.readTree(r.body());
        assertEquals(status, json.get("status").asInt()); assertTrue(json.get("message").isTextual());
    }
    private DevolucionDTO solicitud(Compra c) throws Exception {
        var r = http("POST", "/devoluciones", new SolicitarDevolucionDTO(c.getId(), c.getClienteId(), c.getEntradasIds(), "Motivo cliente"));
        assertEquals(201, r.statusCode(), r.body()); var d = mapper.readValue(r.body(), DevolucionDTO.class);
        assertEquals("/devoluciones/" + d.id(), r.headers().firstValue("Location").orElseThrow()); return d;
    }
    private String ruta(DevolucionDTO d, String accion) { return "/admin/devoluciones/" + d.id() + "/" + accion; }
    @Test void circuitoHttpAprobacionReintegroYConsultasSerializaSoloDTO() throws Exception {
        var c = crear(true); var d = solicitud(c); var aprobar = http("POST", ruta(d,"aprobar"), new AprobarDevolucionDTO("Motivo admin"));
        assertEquals(200, aprobar.statusCode(), aprobar.body()); assertEquals("Motivo admin", mapper.readTree(aprobar.body()).get("motivoResolucion").asText());
        var datos = new ProcesarDevolucionDTO("http-devolucion", d.importeSolicitado()); var r = http("POST", ruta(d,"procesar"), datos);
        assertEquals(200, r.statusCode(), r.body()); var json = mapper.readTree(r.body());
        assertEquals("COMPLETADA", json.at("/devolucion/estado").asText()); assertFalse(json.at("/devolucion").has("compra"));
        assertFalse(json.at("/reintegro").has("pago")); assertFalse(json.toString().contains("hibernateLazyInitializer"));
        assertEquals(200, http("POST", ruta(d,"procesar"), datos).statusCode());
        assertEquals(200, http("GET", ruta(d,"procesamiento"), null).statusCode());
        assertEquals(200, http("GET", "/admin/reintegros/" + json.at("/reintegro/id").asLong(), null).statusCode());
        assertEquals(200, http("GET", "/devoluciones/" + d.id(), null).statusCode());
        var lista = http("GET", "/admin/devoluciones?estado=COMPLETADA", null); assertEquals(200, lista.statusCode()); assertEquals(1, mapper.readTree(lista.body()).size());
        error(http("POST", ruta(d,"procesar"), new ProcesarDevolucionDTO("otra", d.importeSolicitado())), 409);
    }
    @Test void rechazoHttpConservaMotivoYBloqueaProcesamiento() throws Exception {
        var d = solicitud(crear(true)); var r = http("POST", ruta(d,"rechazar"), new RechazarDevolucionDTO("Motivo rechazo"));
        assertEquals(200,r.statusCode(),r.body()); assertEquals("Motivo rechazo", mapper.readTree(r.body()).get("motivoRechazo").asText());
        error(http("POST", ruta(d,"aprobar"), new AprobarDevolucionDTO("No")),409);
        error(http("POST", ruta(d,"procesar"), new ProcesarDevolucionDTO("x",d.importeSolicitado())),409);
    }
    @Test void datosInvalidosYRecursosInexistentesUsanErroresUniformes() throws Exception {
        error(http("GET", "/devoluciones/9223372036854775807", null),404);
        error(http("GET", "/admin/reintegros/9223372036854775807", null),404);
        error(http("GET", "/admin/devoluciones/9223372036854775807/procesamiento", null),404);
        error(http("POST", "/admin/eventos/999/cancelacion", Map.of()),404);
        error(http("GET", "/admin/devoluciones?estado=INVENTADO", null),400);
        error(http("GET", "/admin/devoluciones", null),400);
        error(http("GET", "/devoluciones/-1", null),400);
        error(http("POST", "/devoluciones", Map.of()),400);
        var d = solicitud(crear(true));
        error(http("POST", ruta(d,"aprobar"), Map.of("motivo"," ")),400);
        error(http("POST", ruta(d,"rechazar"), Map.of()),400);
        error(http("POST", ruta(d,"procesar"), Map.of("importe",0,"claveIdempotencia","x")),400);
    }
    @Test void falloExternoDevuelve503YPuedeRecuperarsePorHttp() throws Exception {
        var d = solicitud(crear(true)); http("POST",ruta(d,"aprobar"),new AprobarDevolucionDTO("Si"));
        doThrow(new ComunicacionPasarelaException("Sin Entradas")).when(entradas).anularEntradas(any(),anyList());
        var datos = new ProcesarDevolucionDTO("http-error", d.importeSolicitado()); error(http("POST",ruta(d,"procesar"),datos),503);
        var estado = http("GET",ruta(d,"procesamiento"),null); assertEquals(200,estado.statusCode());
        assertEquals("ERROR_PENDIENTE",mapper.readTree(estado.body()).at("/devolucion/estado").asText());
        reset(entradas); assertEquals(200,http("POST",ruta(d,"procesar"),datos).statusCode()); assertEquals(1,reintegros.count());
    }
    @Test void notificacionHttpDeCancelacionRepetidaNoDuplica() throws Exception {
        crear(true); var r = http("POST","/admin/eventos/20/cancelacion",Map.of()); assertEquals(200,r.statusCode(),r.body());
        assertTrue(mapper.readTree(r.body()).get("comprasPendientes").isEmpty());
        assertEquals(200,http("POST","/admin/eventos/20/cancelacion",Map.of()).statusCode()); assertEquals(1,reintegros.count());
    }
    @Test void cancelacionHttpConFallosDevuelve202YRecupera() throws Exception {
        crear(true); doThrow(new ComunicacionPasarelaException("Sin Entradas")).when(entradas).anularEntradas(any(),anyList());
        var r = http("POST","/admin/eventos/20/cancelacion",Map.of()); assertEquals(202,r.statusCode(),r.body());
        assertEquals(1,mapper.readTree(r.body()).get("comprasPendientes").size()); reset(entradas);
        assertEquals(200,http("POST","/admin/eventos/20/cancelacion",Map.of()).statusCode()); assertEquals(1,reintegros.count());
    }
    @Test void swaggerPublicaTodosLosEndpointsYLosContratosSinEntidades() throws Exception {
        var r = http("GET","/v3/api-docs",null); assertEquals(200,r.statusCode()); var json = mapper.readTree(r.body());
        for (String ruta : List.of("/devoluciones","/devoluciones/{id}","/admin/devoluciones", "/admin/devoluciones/{id}/aprobar",
                "/admin/devoluciones/{id}/rechazar","/admin/devoluciones/{id}/procesar","/admin/devoluciones/{id}/procesamiento",
                "/admin/reintegros/{id}","/admin/eventos/{id}/cancelacion")) assertTrue(json.get("paths").has(ruta),ruta);
        var schemas = json.at("/components/schemas"); assertTrue(schemas.has("ProcesarDevolucionDTO")); assertTrue(schemas.has("ReintegroDTO"));
        assertFalse(schemas.has("Reintegro")); assertFalse(schemas.has("Devolucion"));
        assertEquals(200,http("GET","/swagger-ui/index.html",null).statusCode());
    }
}
