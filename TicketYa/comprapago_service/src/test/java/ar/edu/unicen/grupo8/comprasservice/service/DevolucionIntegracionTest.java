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

@SpringBootTest
@ActiveProfiles("test")
class DevolucionIntegracionTest {
    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");
    @Autowired DevolucionService servicio;
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
    private SolicitarDevolucionDTO solicitud(Compra c, Long... ids) {
        return new SolicitarDevolucionDTO(c.getId(), c.getClienteId(), List.of(ids), "No puedo asistir");
    }
    private DevolucionDTO solicitarUna(Compra c) { return servicio.solicitar(solicitud(c, c.getEntradasIds().get(0))); }
    private void verificarSinEfectos() {
        verify(entradas, never()).anularEntradas(any()); verify(entradas, never()).solicitarEntradas(any(), any(), any(), anyList());
        verify(reservas, never()).liberarReserva(any()); verify(reservas, never()).compensarReserva(any());
        verify(pasarela, never()).procesarPago(any(), any()); verify(pasarela, never()).reintegrarPago(any(), any());
        assertEquals(0, reintegros.count());
    }

    @Test void solicitudParcialPersisteRelacionEImporteHistoricoSinEfectosFinancieros() {
        Compra c = crear(true);
        doAnswer(i -> { assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); return i.callRealMethod(); })
                .when(entradas).consultarEntradas(c.getId());
        doAnswer(i -> { assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); return i.callRealMethod(); })
                .when(eventos).obtenerFechaInicio(20L);
        DevolucionDTO d = solicitarUna(c); var recuperada = servicio.obtener(d.id());
        assertEquals(EstadoDevolucion.PENDIENTE, recuperada.estado()); assertEquals(c.getId(), recuperada.compraId());
        assertEquals(10L, recuperada.clienteId()); assertEquals(20L, recuperada.eventoId());
        assertEquals(LocalDateTime.ofInstant(AHORA, ZoneOffset.UTC), recuperada.fechaSolicitud());
        assertEquals(0, new BigDecimal("15000.00").compareTo(recuperada.importeSolicitado()));
        assertEquals(0, recuperada.importeReintegrado().signum()); assertEquals(1, recuperada.detalles().size());
        assertEquals(c.getDetalles().get(0).getId(), recuperada.detalles().get(0).detalleCompraId());
        assertEquals(c.getEntradasIds().get(0), recuperada.detalles().get(0).entradaId());
        assertEquals("No puedo asistir", recuperada.motivo()); assertNull(recuperada.fechaResolucion());
        assertEquals(EstadoCompra.COMPLETADA, compras.obtenerCompraPorId(c.getId()).getEstado());
        verify(eventos, never()).obtenerPrecioSector(any(), any()); verificarSinEfectos();
    }
    @Test void solicitudTotalSumaSoloPreciosPagados() {
        Compra c = crear(true); var d = servicio.solicitar(solicitud(c, c.getEntradasIds().toArray(Long[]::new)));
        assertEquals(2, d.detalles().size()); assertEquals(0, c.getImporteTotal().compareTo(d.importeSolicitado()));
        assertEquals(0, d.importeReintegrado().signum()); verificarSinEfectos();
    }
    @Test void permiteMasDe72Horas() {
        Compra c = crear(true); doReturn(LocalDateTime.now(clock).plusHours(72).plusSeconds(1)).when(eventos).obtenerFechaInicio(20L);
        assertEquals(EstadoDevolucion.PENDIENTE, solicitarUna(c).estado());
    }
    @Test void permiteExactamente72Horas() {
        Compra c = crear(true); doReturn(LocalDateTime.now(clock).plusHours(72)).when(eventos).obtenerFechaInicio(20L);
        assertEquals(EstadoDevolucion.PENDIENTE, solicitarUna(c).estado());
    }
    @Test void rechazaMenosDe72HorasSinPersistenciaParcial() {
        Compra c = crear(true); doReturn(LocalDateTime.now(clock).plusHours(72).minusNanos(1)).when(eventos).obtenerFechaInicio(20L);
        assertThrows(IllegalArgumentException.class, () -> solicitarUna(c)); assertEquals(0, devoluciones.count()); assertEquals(0, detalles.count());
    }
    @Test void fechaEventoDesconocidaNoHabilitaSolicitud() {
        Compra c = crear(true); doReturn(null).when(eventos).obtenerFechaInicio(20L);
        assertThrows(IllegalStateException.class, () -> solicitarUna(c)); assertEquals(0, devoluciones.count());
    }
    @Test void compraInexistenteSeDistingueDeDatosInvalidos() {
        assertThrows(CompraNoEncontradaException.class, () -> servicio.solicitar(new SolicitarDevolucionDTO(Long.MAX_VALUE, 10L, List.of(1L), "Motivo")));
        verify(entradas, never()).consultarEntradas(any()); assertEquals(0, devoluciones.count());
    }
    @Test void compraAjenaOPendienteNoPermiteSolicitud() {
        Compra c = crear(false);
        assertThrows(IllegalArgumentException.class, () -> servicio.solicitar(new SolicitarDevolucionDTO(c.getId(), 99L, List.of(1L), "Motivo")));
        assertThrows(IllegalStateException.class, () -> servicio.solicitar(solicitud(c, 1L)));
        assertEquals(0, devoluciones.count()); verify(entradas, never()).consultarEntradas(any());
    }
    @Test void entradaAjenaYSeleccionRepetidaSonRechazadas() {
        Compra c = crear(true); assertThrows(IllegalArgumentException.class, () -> servicio.solicitar(solicitud(c, Long.MAX_VALUE)));
        Long id = c.getEntradasIds().get(0);
        assertThrows(IllegalArgumentException.class, () -> servicio.solicitar(solicitud(c, id, id)));
        assertEquals(0, devoluciones.count());
    }
    @Test void entradaNoVigenteEsRechazada() {
        Compra c = crear(true); var originales = entradas.consultarEntradas(c.getId()); var e = originales.get(0);
        for (String estado : List.of("ANULADA", "USADA")) {
            var invalida = new EntradaDTO(e.entradaId(), e.compraId(), e.eventoId(), e.clienteId(), e.sectorId(), e.asientoId(), estado, e.codigo());
            doReturn(List.of(invalida, originales.get(1))).when(entradas).consultarEntradas(c.getId());
            assertThrows(IllegalStateException.class, () -> servicio.solicitar(solicitud(c, e.entradaId())));
        }
        assertEquals(0, devoluciones.count()); verificarSinEfectos();
    }
    @Test void respuestaDeEntradasIncompatibleOVaciaNoSeAcepta() {
        Compra c = crear(true); var originales = entradas.consultarEntradas(c.getId()); var e = originales.get(0);
        var ajena = new EntradaDTO(e.entradaId(), e.compraId(), e.eventoId(), 999L, e.sectorId(), e.asientoId(), e.estado(), e.codigo());
        doReturn(List.of(ajena, originales.get(1))).when(entradas).consultarEntradas(c.getId());
        assertThrows(IllegalStateException.class, () -> solicitarUna(c));
        doReturn(List.of()).when(entradas).consultarEntradas(c.getId());
        assertThrows(IllegalStateException.class, () -> solicitarUna(c)); assertEquals(0, devoluciones.count());
    }
    @Test void solicitudesPendientesYAprobadasBloqueanLaMismaEntrada() {
        Compra c = crear(true); var d = solicitarUna(c);
        assertThrows(IllegalStateException.class, () -> solicitarUna(c));
        servicio.aprobar(d.id(), new AprobarDevolucionDTO("Cumple las condiciones"));
        assertThrows(IllegalStateException.class, () -> solicitarUna(c)); assertEquals(1, devoluciones.count());
    }
    @Test void rechazoPermiteOtraSolicitudMientrasSigaEnPlazo() {
        Compra c = crear(true); var primera = solicitarUna(c);
        servicio.rechazar(primera.id(), new RechazarDevolucionDTO("Documentacion insuficiente"));
        var segunda = solicitarUna(c);
        assertNotEquals(primera.id(), segunda.id()); assertEquals(EstadoDevolucion.PENDIENTE, segunda.estado());
        assertEquals(EstadoDevolucion.RECHAZADA, servicio.obtener(primera.id()).estado()); assertEquals(2, detalles.count());
    }
    @Test void consultasDevuelvenSeleccionYFiltranEstadoSinEntidadesJpa() {
        Compra c = crear(true); var primera = solicitarUna(c);
        var segunda = servicio.solicitar(solicitud(c, c.getEntradasIds().get(1)));
        servicio.rechazar(segunda.id(), new RechazarDevolucionDTO("No corresponde"));
        var pendientes = servicio.obtenerPorEstado(EstadoDevolucion.PENDIENTE);
        assertEquals(1, pendientes.size()); assertEquals(primera.id(), pendientes.get(0).id());
        assertEquals(1, servicio.obtenerPorEstado(EstadoDevolucion.RECHAZADA).size());
        assertTrue(servicio.obtenerPorEstado(EstadoDevolucion.APROBADA).isEmpty());
        var json = mapper.readTree(mapper.writeValueAsString(servicio.obtener(primera.id())));
        assertTrue(json.has("compraId")); assertFalse(json.has("compra")); assertFalse(json.get("detalles").get(0).has("detalleCompra"));
        assertThrows(UnsupportedOperationException.class, () -> pendientes.get(0).detalles().clear());
    }
    @Test void devolucionInexistenteEsRechazadaEnConsultaYAprobacion() {
        assertThrows(DevolucionNoEncontradaException.class, () -> servicio.obtener(Long.MAX_VALUE));
        assertThrows(DevolucionNoEncontradaException.class, () -> servicio.aprobar(Long.MAX_VALUE, new AprobarDevolucionDTO("Motivo")));
    }
    @Test void aprobacionEsIdempotenteConservaMotivoClienteYFechaDeDecision() {
        Compra c = crear(true); var d = solicitarUna(c);
        var aprobada = servicio.aprobar(d.id(), new AprobarDevolucionDTO("Cumple condiciones"));
        when(clock.instant()).thenReturn(AHORA.plus(Duration.ofDays(5)));
        var repetida = servicio.aprobar(d.id(), new AprobarDevolucionDTO("Cumple condiciones"));
        assertEquals(EstadoDevolucion.APROBADA, repetida.estado()); assertEquals(aprobada.fechaResolucion(), repetida.fechaResolucion());
        assertEquals("No puedo asistir", repetida.motivo()); assertEquals("Cumple condiciones", repetida.motivoResolucion());
        assertNull(repetida.motivoRechazo()); assertEquals(0, repetida.importeReintegrado().signum());
        assertThrows(IllegalStateException.class, () -> servicio.aprobar(d.id(), new AprobarDevolucionDTO("Otro motivo")));
        assertEquals("Cumple condiciones", servicio.obtener(d.id()).motivoResolucion()); verificarSinEfectos();
    }
    @Test void rechazoEsIdempotenteYNoPermiteAprobarDespues() {
        Compra c = crear(true); var d = solicitarUna(c);
        var rechazada = servicio.rechazar(d.id(), new RechazarDevolucionDTO("No corresponde"));
        var repetida = servicio.rechazar(d.id(), new RechazarDevolucionDTO("No corresponde"));
        assertEquals(rechazada.fechaResolucion(), repetida.fechaResolucion()); assertEquals("No corresponde", repetida.motivoRechazo());
        assertEquals("No puedo asistir", repetida.motivo());
        assertThrows(IllegalStateException.class, () -> servicio.aprobar(d.id(), new AprobarDevolucionDTO("Intento invalido")));
        assertThrows(IllegalStateException.class, () -> servicio.rechazar(d.id(), new RechazarDevolucionDTO("Otro motivo")));
        verificarSinEfectos();
    }
    @Test void aprobadaNoPuedeRechazarseYPlazoSeEvaluaAlSolicitar() {
        Compra c = crear(true); var d = solicitarUna(c);
        when(clock.instant()).thenReturn(AHORA.plus(Duration.ofDays(5)));
        servicio.aprobar(d.id(), new AprobarDevolucionDTO("Solicitada dentro del plazo"));
        assertThrows(IllegalStateException.class, () -> servicio.rechazar(d.id(), new RechazarDevolucionDTO("Cambio de decision")));
        assertEquals(EstadoDevolucion.APROBADA, servicio.obtener(d.id()).estado());
    }
    @Test void validacionDeServicioImpideDtosInvalidos() {
        assertThrows(ConstraintViolationException.class, () -> servicio.solicitar(new SolicitarDevolucionDTO(null, 10L, List.of(1L), "Motivo")));
        assertThrows(ConstraintViolationException.class, () -> servicio.aprobar(1L, new AprobarDevolucionDTO(" ")));
        assertThrows(ConstraintViolationException.class, () -> servicio.rechazar(1L, new RechazarDevolucionDTO(null)));
        assertThrows(ConstraintViolationException.class, () -> servicio.obtener(0L));
        assertThrows(ConstraintViolationException.class, () -> servicio.obtenerPorEstado(null)); assertEquals(0, devoluciones.count());
    }
    @Test void solicitudesConcurrentesParaLaMismaEntradaRegistranSoloUna() throws Exception {
        Compra c = crear(true); var dto = solicitud(c, c.getEntradasIds().get(0));
        CountDownLatch inicio = new CountDownLatch(1); ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> accion = () -> { inicio.await(); try { servicio.solicitar(dto); return true; } catch (IllegalStateException ex) { return false; } };
            Future<Boolean> a = ejecutor.submit(accion), b = ejecutor.submit(accion); inicio.countDown();
            assertNotEquals(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertEquals(1, devoluciones.count()); assertEquals(1, detalles.count());
        } finally { ejecutor.shutdownNow(); }
    }
    @Test void aprobacionYRechazoConcurrentesConservanUnaSolaDecision() throws Exception {
        Compra c = crear(true); var d = solicitarUna(c);
        CountDownLatch inicio = new CountDownLatch(1); ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = ejecutor.submit(() -> { inicio.await(); try { servicio.aprobar(d.id(), new AprobarDevolucionDTO("Aprobar")); return true; } catch (IllegalStateException ex) { return false; } });
            Future<Boolean> b = ejecutor.submit(() -> { inicio.await(); try { servicio.rechazar(d.id(), new RechazarDevolucionDTO("Rechazar")); return true; } catch (IllegalStateException ex) { return false; } });
            inicio.countDown(); boolean aprobada = a.get(20, TimeUnit.SECONDS), rechazada = b.get(20, TimeUnit.SECONDS);
            assertNotEquals(aprobada, rechazada); var resultado = servicio.obtener(d.id());
            assertEquals(aprobada ? EstadoDevolucion.APROBADA : EstadoDevolucion.RECHAZADA, resultado.estado());
            assertEquals(aprobada ? "Aprobar" : "Rechazar", resultado.motivoResolucion()); assertNotNull(resultado.fechaResolucion());
        } finally { ejecutor.shutdownNow(); }
        verificarSinEfectos();
    }
    @Test void preciosHistoricosAmbiguosSeRechazanSinElegirUnPrecioArbitrario() {
        Compra c = crear(true);
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            Compra entidad = compraRepository.findById(c.getId()).orElseThrow();
            DetalleCompra extra = new DetalleCompra(); extra.setSectorId(1L); extra.setCantidad(1);
            extra.setPrecioUnitario(new BigDecimal("20000.00")); extra.setSubtotal(new BigDecimal("20000.00")); entidad.agregarDetalle(extra);
            compraRepository.saveAndFlush(entidad);
        });
        var error = assertThrows(IllegalStateException.class, () -> solicitarUna(c));
        assertTrue(error.getMessage().contains("ambigua")); assertEquals(0, devoluciones.count());
    }
    @Test void detallesEquivalentesSeAsocianDeFormaEstableEntreSolicitudesParciales() {
        Compra c = crear(true);
        Long nuevoDetalle = new TransactionTemplate(manager).execute(s -> {
            Compra entidad = compraRepository.findById(c.getId()).orElseThrow();
            entidad.getDetalles().get(0).setCantidad(1); entidad.getDetalles().get(0).setSubtotal(new BigDecimal("15000.00"));
            DetalleCompra extra = new DetalleCompra(); extra.setSectorId(1L); extra.setCantidad(1);
            extra.setPrecioUnitario(new BigDecimal("15000.00")); extra.setSubtotal(new BigDecimal("15000.00")); entidad.agregarDetalle(extra);
            Compra guardada = compraRepository.saveAndFlush(entidad);
            return guardada.getDetalles().stream().map(DetalleCompra::getId)
                    .filter(id -> !id.equals(c.getDetalles().get(0).getId())).findFirst().orElseThrow();
        });
        assertNotNull(nuevoDetalle);
        var ids = c.getEntradasIds().stream().sorted().toList();
        var segunda = servicio.solicitar(solicitud(c, ids.get(1))); var primera = servicio.solicitar(solicitud(c, ids.get(0)));
        assertEquals(nuevoDetalle, segunda.detalles().get(0).detalleCompraId());
        assertEquals(c.getDetalles().get(0).getId(), primera.detalles().get(0).detalleCompraId());
        assertEquals(0, primera.importeSolicitado().compareTo(segunda.importeSolicitado()));
    }
    @Test void devolucionCompletadaNoPermiteOtraSolicitudNiDecision() {
        Compra c = crear(true); var d = solicitarUna(c); servicio.aprobar(d.id(), new AprobarDevolucionDTO("Cumple"));
        // Fixture del estado futuro: no ejecuta procesamiento de reintegro ni anulación.
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            var entidad = devoluciones.findById(d.id()).orElseThrow();
            entidad.setEstado(EstadoDevolucion.PROCESANDO); entidad.setEstado(EstadoDevolucion.COMPLETADA);
        });
        assertThrows(IllegalStateException.class, () -> solicitarUna(c));
        assertThrows(IllegalStateException.class, () -> servicio.rechazar(d.id(), new RechazarDevolucionDTO("No permitido")));
        assertThrows(IllegalStateException.class, () -> servicio.aprobar(d.id(), new AprobarDevolucionDTO("No permitido")));
    }

    @Test void nuevaSolicitudTrasRechazoDebeVolverACumplirElPlazo() {
        Compra c = crear(true); var d = solicitarUna(c);
        servicio.rechazar(d.id(), new RechazarDevolucionDTO("Rechazada"));
        when(clock.instant()).thenReturn(AHORA.plus(Duration.ofDays(6)));
        assertThrows(IllegalArgumentException.class, () -> solicitarUna(c));
        assertEquals(1, devoluciones.count()); assertEquals(EstadoDevolucion.RECHAZADA, servicio.obtener(d.id()).estado());
    }
}
