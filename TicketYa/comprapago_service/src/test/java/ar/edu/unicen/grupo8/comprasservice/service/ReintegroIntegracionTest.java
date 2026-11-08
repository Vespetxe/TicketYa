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
class ReintegroIntegracionTest {
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
    private DevolucionDTO solicitar(Compra c, List<Long> ids) {
        return servicio.solicitar(new SolicitarDevolucionDTO(c.getId(), c.getClienteId(), ids, "No puedo asistir"));
    }
    private DevolucionDTO aprobada(Compra c, List<Long> ids) {
        var d = solicitar(c, ids); return servicio.aprobar(d.id(), new AprobarDevolucionDTO("Corresponde"));
    }
    private ProcesarDevolucionDTO datos(DevolucionDTO d) { return new ProcesarDevolucionDTO("devolucion-" + d.id(), d.importeSolicitado()); }
    private ResultadoDevolucionDTO procesar(DevolucionDTO d) { return flujo.procesar(d.id(), datos(d)); }
    private void completada(ResultadoDevolucionDTO r) {
        assertEquals(EstadoDevolucion.COMPLETADA, r.devolucion().estado());
        assertEquals(EtapaDevolucion.FINALIZADA, r.etapa()); assertEquals(EstadoReintegro.COMPLETADO, r.reintegro().estado());
        assertEquals(0, r.devolucion().importeSolicitado().compareTo(r.devolucion().importeReintegrado()));
        assertNotNull(r.reintegro().fechaProcesamiento()); assertNotNull(r.reintegro().referenciaExterna());
    }
    @Test void parcialesPorEntradasAcumulanHastaTotalSinAfectarLasRestantes() {
        var c = crear(true); var primera = aprobada(c, List.of(c.getEntradasIds().get(0)));
        var r = procesar(primera); completada(r);
        assertEquals(EstadoCompra.DEVUELTA_PARCIAL, compras.obtenerCompraPorId(c.getId()).getEstado());
        assertEquals(List.of("ANULADA", "EMITIDA"), entradas.consultarEntradas(c.getId()).stream().map(EntradaDTO::estado).toList());
        assertEquals(List.of(c.getEntradasIds().get(0)), reservas.consultarLugaresLiberados(c.getReservaId()));
        var segunda = aprobada(c, List.of(c.getEntradasIds().get(1))); completada(procesar(segunda));
        assertEquals(EstadoCompra.DEVUELTA_TOTAL, compras.obtenerCompraPorId(c.getId()).getEstado());
        var p = pagos.obtenerPagosPorCompra(c.getId()).get(0);
        assertEquals(EstadoPago.REINTEGRADO_TOTAL, p.getEstado()); assertEquals(0, p.getImporte().compareTo(p.getImporteReintegrado()));
        assertEquals(2, reintegros.count()); assertEquals(2, reservas.consultarLugaresLiberados(c.getReservaId()).size());
        verify(entradas, never()).anularEntradas(any()); verify(reservas, never()).compensarReserva(any());
    }
    @Test void totalRepetidoConservaIdentidadFechasYUnSoloEfecto() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds()); var r = procesar(d); completada(r);
        assertEquals(r, procesar(d)); assertEquals(r.reintegro(), flujo.obtenerReintegro(r.reintegro().id()));
        verify(pasarela, times(1)).reintegrarPago("pago-devolucion", datos(d).claveIdempotencia(), d.importeSolicitado());
        verify(entradas, times(1)).anularEntradas(eq(c.getId()), argThat(ids -> new HashSet<>(ids).equals(new HashSet<>(c.getEntradasIds())))); verify(reservas, times(1)).liberarLugares(eq(c.getReservaId()), anyList());
        assertThrows(IllegalStateException.class, () -> flujo.procesar(d.id(), new ProcesarDevolucionDTO("otra", d.importeSolicitado())));
        assertThrows(IllegalStateException.class, () -> flujo.procesar(d.id(), new ProcesarDevolucionDTO(datos(d).claveIdempotencia(), BigDecimal.ONE)));
    }
    @Test void requiereAprobacionImporteCompletoYSaldoPagado() {
        var c = crear(true); var d = solicitar(c, c.getEntradasIds());
        assertThrows(IllegalStateException.class, () -> procesar(d)); servicio.aprobar(d.id(), new AprobarDevolucionDTO("Si"));
        for (var importe : List.of(BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("0.001")))
            assertThrows(ConstraintViolationException.class, () -> flujo.procesar(d.id(), new ProcesarDevolucionDTO("x", importe)));
        assertThrows(IllegalArgumentException.class, () -> flujo.procesar(d.id(), new ProcesarDevolucionDTO("x", BigDecimal.ONE)));
        new TransactionTemplate(manager).executeWithoutResult(s -> {
            var p = pagoRepository.findByCompraId(c.getId()).get(0); p.setImporte(new BigDecimal("10000"));
        });
        assertThrows(IllegalStateException.class, () -> procesar(d)); assertEquals(0, reintegros.count());
        verify(pasarela, never()).reintegrarPago(anyString(), anyString(), any());
    }
    @Test void claveNoPuedeUsarseParaOtraDevolucion() {
        var c = crear(true); var a = aprobada(c, List.of(c.getEntradasIds().get(0))); var z = aprobada(c, List.of(c.getEntradasIds().get(1)));
        completada(procesar(a));
        assertThrows(IllegalStateException.class, () -> flujo.procesar(z.id(), new ProcesarDevolucionDTO(datos(a).claveIdempotencia(), z.importeSolicitado())));
        assertEquals(EstadoDevolucion.APROBADA, servicio.obtener(z.id()).estado()); assertEquals(1, reintegros.count());
    }
    @Test void respuestaPerdidaSeConsultaInclusoAlReconstruirElCliente() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doAnswer(i -> { i.callRealMethod(); throw new ComunicacionPasarelaException("Respuesta perdida"); })
                .when(pasarela).reintegrarPago(anyString(), anyString(), any());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d));
        var incompleta = flujo.obtener(d.id()); assertEquals(EstadoDevolucion.ERROR_PENDIENTE, incompleta.devolucion().estado());
        assertEquals(EstadoReintegro.RESULTADO_DESCONOCIDO, incompleta.reintegro().estado()); assertEquals(EtapaDevolucion.REINTEGRO, incompleta.etapa());
        assertNotNull(new PasarelaPagoSimuladaService(almacen).consultarReintegro("pago-devolucion", datos(d).claveIdempotencia()));
        completada(procesar(d)); verify(pasarela, times(1)).reintegrarPago(anyString(), anyString(), any()); assertEquals(1, reintegros.count());
    }
    @Test void consultaFallidaNoHabilitaReenvioNiCompensacion() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doThrow(new ComunicacionPasarelaException("Sin consulta")).when(pasarela).consultarReintegro(anyString(), anyString());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d));
        verify(pasarela, never()).reintegrarPago(anyString(), anyString(), any()); verify(entradas, never()).anularEntradas(any(), anyList());
        reset(pasarela); completada(procesar(d));
    }
    @Test void rechazoDefinitivoPersisteYNoReenviaAutomaticamente() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doReturn(new ResultadoPasarela(false, "RECHAZO")).when(pasarela).reintegrarPago(anyString(), anyString(), any());
        assertThrows(IllegalStateException.class, () -> procesar(d));
        assertEquals(EstadoReintegro.RECHAZADO, flujo.obtener(d.id()).reintegro().estado());
        assertThrows(IllegalStateException.class, () -> procesar(d)); verify(pasarela, times(1)).reintegrarPago(anyString(), anyString(), any());
        assertEquals(EstadoCompra.COMPLETADA, compras.obtenerCompraPorId(c.getId()).getEstado()); verify(entradas, never()).anularEntradas(any(), anyList());
    }
    @Test void anulacionFallidaConservaReintegroYRecuperaSinReembolsarOtraVez() {
        var c = crear(true); var d = aprobada(c, List.of(c.getEntradasIds().get(0)));
        doThrow(new ComunicacionPasarelaException("Sin Entradas")).when(entradas).anularEntradas(any(), anyList());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d));
        assertEquals(EtapaDevolucion.ANULACION, flujo.obtener(d.id()).etapa()); assertEquals(EstadoReintegro.COMPLETADO, flujo.obtener(d.id()).reintegro().estado());
        verify(reservas, never()).liberarLugares(any(), anyList()); reset(entradas); completada(procesar(d));
        verify(pasarela, times(1)).reintegrarPago(anyString(), anyString(), any());
    }
    @Test void respuestaDeAnulacionPerdidaNoRepiteElEfecto() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doAnswer(i -> { i.callRealMethod(); throw new ComunicacionPasarelaException("Respuesta perdida"); }).when(entradas).anularEntradas(any(), anyList());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d)); completada(procesar(d));
        verify(entradas, times(1)).anularEntradas(any(), anyList());
        assertThrows(OperacionExternaRechazadaException.class, () -> entradas.solicitarEntradas(c.getId(), c.getEventoId(), c.getClienteId(), List.of(new DetalleEmisionDTO(1L, null, 2))));
    }
    @Test void liberacionFallidaSeRecuperaEnSuEtapaSinRepetirAnulacion() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doThrow(new ComunicacionPasarelaException("Sin Reservas")).when(reservas).liberarLugares(any(), anyList());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d)); assertEquals(EtapaDevolucion.LIBERACION, flujo.obtener(d.id()).etapa());
        reset(reservas); completada(procesar(d)); verify(entradas, times(1)).anularEntradas(any(), anyList());
        verify(pasarela, times(1)).reintegrarPago(anyString(), anyString(), any());
    }
    @Test void respuestaDeLiberacionPerdidaSeResuelveConsultando() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doAnswer(i -> { i.callRealMethod(); throw new ComunicacionPasarelaException("Respuesta perdida"); }).when(reservas).liberarLugares(any(), anyList());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d)); completada(procesar(d));
        verify(reservas, times(1)).liberarLugares(any(), anyList()); assertEquals(2, reservas.consultarLugaresLiberados(c.getReservaId()).size());
    }
    @Test void noFinalizaSiEntradasNoConfirmaLaAnulacion() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds()); doNothing().when(entradas).anularEntradas(any(), anyList());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d)); assertEquals(EtapaDevolucion.ANULACION, flujo.obtener(d.id()).etapa());
        verify(reservas, never()).liberarLugares(any(), anyList());
    }
    @Test void noFinalizaSiReservasNoConfirmaLaLiberacion() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds()); doNothing().when(reservas).liberarLugares(any(), anyList());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d)); assertEquals(EtapaDevolucion.LIBERACION, flujo.obtener(d.id()).etapa());
    }
    @Test void turnoVencidoPermiteRecuperarYRechazaAlEjecutorAnterior() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds()); var viejo = registro.tomar(d.id(), datos(d));
        assertThrows(IllegalStateException.class, () -> procesar(d));
        when(clock.instant()).thenReturn(AHORA.plusSeconds(121)); completada(procesar(d));
        assertThrows(IllegalStateException.class, () -> registro.registrarResultado(d.id(), viejo.token(), new ResultadoPasarela(true, "viejo")));
        assertEquals(1, reintegros.count());
    }
    @Test void concurrenciaMismaCompraNoDuplicaNiPermiteDosEjecutores() throws Exception {
        var c = crear(true); var a = aprobada(c, List.of(c.getEntradasIds().get(0))); var z = aprobada(c, List.of(c.getEntradasIds().get(1)));
        var entro = new CountDownLatch(1); var seguir = new CountDownLatch(1);
        doAnswer(i -> { entro.countDown(); assertTrue(seguir.await(10, TimeUnit.SECONDS)); return i.callRealMethod(); })
                .when(pasarela).reintegrarPago(anyString(), anyString(), any());
        try (var executor = Executors.newSingleThreadExecutor()) {
            var primera = executor.submit(() -> procesar(a));
            try {
                assertTrue(entro.await(10, TimeUnit.SECONDS)); assertThrows(IllegalStateException.class, () -> procesar(a));
                assertThrows(IllegalStateException.class, () -> procesar(z));
            } finally { seguir.countDown(); }
            completada(primera.get(15, TimeUnit.SECONDS));
        }
        completada(procesar(z)); assertEquals(2, reintegros.count()); completada(procesar(a));
    }
    @Test void ningunaOperacionExternaComparteTransaccionLocal() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doAnswer(i -> { assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); return i.callRealMethod(); }).when(pasarela).reintegrarPago(anyString(), anyString(), any());
        doAnswer(i -> { assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); return i.callRealMethod(); }).when(entradas).anularEntradas(any(), anyList());
        doAnswer(i -> { assertFalse(TransactionSynchronizationManager.isActualTransactionActive()); return i.callRealMethod(); }).when(reservas).liberarLugares(any(), anyList());
        completada(procesar(d));
    }
    @Test void cancelacionReutilizaPendienteYOmitiendo72HorasNoReofreceLugares() {
        var c = crear(true); var pendiente = solicitar(c, List.of(c.getEntradasIds().get(0)));
        doReturn(LocalDateTime.now(clock).plusHours(1)).when(eventos).obtenerFechaInicio(20L);
        var r = flujo.cancelarEvento(20L); assertTrue(r.comprasPendientes().isEmpty()); assertEquals(2, r.devoluciones().size());
        r.devoluciones().forEach(this::completada); assertEquals(EstadoDevolucion.COMPLETADA, servicio.obtener(pendiente.id()).estado());
        assertTrue(r.devoluciones().stream().allMatch(ResultadoDevolucionDTO::cancelacionEvento)); assertEquals(2, reintegros.count());
        assertFalse(eventos.eventoValido(20L)); assertTrue(reservas.consultarLugaresLiberados(c.getReservaId()).isEmpty());
        var repetida = flujo.cancelarEvento(20L); assertTrue(repetida.comprasPendientes().isEmpty()); assertEquals(2, devoluciones.count()); assertEquals(2, reintegros.count());
        verify(reservas, never()).liberarLugares(any(), anyList());
    }
    @Test void cancelacionConDevolucionPreviaYRechazoConservaHistorialYSoloReintegraRestante() {
        var c = crear(true); var parcial = aprobada(c, List.of(c.getEntradasIds().get(0))); completada(procesar(parcial));
        var rechazada = solicitar(c, List.of(c.getEntradasIds().get(1))); servicio.rechazar(rechazada.id(), new RechazarDevolucionDTO("No corresponde"));
        var r = flujo.cancelarEvento(20L); assertTrue(r.comprasPendientes().isEmpty());
        assertEquals(EstadoDevolucion.RECHAZADA, servicio.obtener(rechazada.id()).estado()); assertEquals(3, devoluciones.count()); assertEquals(2, reintegros.count());
        assertEquals(EstadoCompra.DEVUELTA_TOTAL, compras.obtenerCompraPorId(c.getId()).getEstado());
        assertEquals(List.of(c.getEntradasIds().get(0)), reservas.consultarLugaresLiberados(c.getReservaId()));
    }
    @Test void cancelacionRecuperaAprobadaYErrorDeAnulacionSinNuevoReintegro() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doThrow(new ComunicacionPasarelaException("Fallo")).when(entradas).anularEntradas(any(), anyList());
        var r = flujo.cancelarEvento(20L); assertEquals(List.of(c.getId()), r.comprasPendientes()); assertEquals(1, reintegros.count());
        reset(entradas); var recuperada = flujo.cancelarEvento(20L); assertTrue(recuperada.comprasPendientes().isEmpty());
        completada(recuperada.devoluciones().get(0)); assertEquals(1, devoluciones.count()); verify(pasarela, times(1)).reintegrarPago(anyString(), anyString(), any());
    }
    @Test void cancelacionProcesaVariasComprasYAislaFalloDeUnaCompra() {
        var c = crear(true);
        reservas.registrarReserva(new ReservaDTO(500L, 11L, 20L, "TEMPORAL", LocalDateTime.now(clock).plusMinutes(15), List.of(new DetalleEmisionDTO(1L, null, 1))));
        var dto = new CrearCompraDTO(); dto.setClienteId(11L); dto.setEventoId(20L); dto.setReservaId(500L);
        var detalle = new DetalleCompraDTO(); detalle.setSectorId(1L); detalle.setCantidad(1); dto.setDetalles(List.of(detalle));
        var segunda = compras.crearCompra(dto); pagos.procesarPago(segunda.getId(), "segundo-pago", MedioPago.TARJETA_DEBITO);
        doThrow(new ComunicacionPasarelaException("Fallo una compra")).when(entradas).anularEntradas(eq(c.getId()), anyList());
        var r = flujo.cancelarEvento(20L); assertEquals(List.of(c.getId()), r.comprasPendientes());
        assertEquals(EstadoCompra.DEVUELTA_TOTAL, compras.obtenerCompraPorId(segunda.getId()).getEstado()); assertEquals(1, r.devoluciones().size());
        reset(entradas); assertTrue(flujo.cancelarEvento(20L).comprasPendientes().isEmpty()); assertEquals(2, reintegros.count());
    }
    @Test void eventoInexistenteNoGeneraOperaciones() {
        assertThrows(EventoNoEncontradoException.class, () -> flujo.cancelarEvento(999L)); assertEquals(0, devoluciones.count());
    }
    @Test void eventoYaIniciadoNoReofreceLugaresAlResolverSolicitudAnterior() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        doReturn(LocalDateTime.now(clock).minusMinutes(1)).when(eventos).obtenerFechaInicio(20L);
        completada(procesar(d)); verify(reservas, never()).liberarLugares(any(), anyList());
    }
    @Test void solicitudAnteriorSinEtapaSeInicializaAntesDeProcesar() {
        var c = crear(true); var d = aprobada(c, c.getEntradasIds());
        new TransactionTemplate(manager).executeWithoutResult(s -> devoluciones.findById(d.id()).orElseThrow().setEtapa(null));
        completada(procesar(d));
    }
    @Test void cancelacionReutilizaLaCancelacionDeCompraSinCobro() {
        var c = crear(false); var r = flujo.cancelarEvento(20L); assertTrue(r.comprasPendientes().isEmpty());
        var cancelada = compras.obtenerCompraPorId(c.getId()); assertEquals(EstadoCompra.CANCELADA, cancelada.getEstado());
        assertEquals(EtapaCompra.FINALIZADA, cancelada.getEtapa()); assertEquals(0, reintegros.count());
        verify(pasarela, never()).procesarPago(any(), any()); assertEquals("LIBERADA", reservas.consultarReserva(c.getReservaId()).estado());
    }
    @Test void cancelacionReportaPagoEnProcesoYLoRecogeTrasRecuperarlo() {
        var c = crear(false); doThrow(new ComunicacionPasarelaException("Sin confirmacion")).when(reservas).confirmarReserva(c.getReservaId());
        pagos.procesarPago(c.getId(), "pago-devolucion", MedioPago.TARJETA_DEBITO);
        var r = flujo.cancelarEvento(20L); assertEquals(List.of(c.getId()), r.comprasPendientes()); assertEquals(0, reintegros.count());
        reset(reservas); pagos.procesarPago(c.getId(), "pago-devolucion", MedioPago.TARJETA_DEBITO);
        var recuperada = flujo.cancelarEvento(20L); assertTrue(recuperada.comprasPendientes().isEmpty());
        completada(recuperada.devoluciones().get(0)); assertEquals(1, reintegros.count());
    }

    private CrearCompraDTO nuevaSolicitud(Long reservaId, int cantidad) {
        reservas.registrarReserva(new ReservaDTO(reservaId, 10L, 20L, "TEMPORAL",
                LocalDateTime.now(Clock.systemUTC()).plusHours(1), List.of(new DetalleEmisionDTO(1L, null, cantidad))));
        var dto = new CrearCompraDTO(); dto.setClienteId(10L); dto.setEventoId(20L); dto.setReservaId(reservaId);
        var detalle = new DetalleCompraDTO(); detalle.setSectorId(1L); detalle.setCantidad(cantidad); dto.setDetalles(List.of(detalle)); return dto;
    }
    @Test void limiteDescuentaParcialConfirmadaUnaSolaVezAunqueExistaRechazoPrevio() {
        var c = crear(true); compras.crearCompra(nuevaSolicitud(600L, 4));
        var ids = List.of(c.getEntradasIds().get(0)); var rechazada = solicitar(c, ids);
        servicio.rechazar(rechazada.id(), new RechazarDevolucionDTO("Revisar solicitud"));
        var d = aprobada(c, ids); completada(procesar(d));
        assertNotNull(compras.crearCompra(nuevaSolicitud(601L, 1)).getId()); completada(procesar(d));
        var excedida = nuevaSolicitud(602L, 1); assertThrows(IllegalArgumentException.class, () -> compras.crearCompra(excedida));
    }
    @Test void limiteLiberaTodaLaCompraConReintegroTotalConfirmado() {
        var c = crear(true); compras.crearCompra(nuevaSolicitud(600L, 4)); completada(procesar(aprobada(c, c.getEntradasIds())));
        assertNotNull(compras.crearCompra(nuevaSolicitud(601L, 2)).getId());
        var excedida = nuevaSolicitud(602L, 1); assertThrows(IllegalArgumentException.class, () -> compras.crearCompra(excedida));
    }
    @Test void solicitudAprobacionYReintegroInciertoNoLiberanLimite() {
        var c = crear(true); compras.crearCompra(nuevaSolicitud(600L, 4));
        var d = solicitar(c, List.of(c.getEntradasIds().get(0))); var nueva = nuevaSolicitud(601L, 1);
        assertThrows(IllegalArgumentException.class, () -> compras.crearCompra(nueva));
        servicio.aprobar(d.id(), new AprobarDevolucionDTO("Corresponde"));
        assertThrows(IllegalArgumentException.class, () -> compras.crearCompra(nueva));
        doAnswer(i -> { i.callRealMethod(); throw new ComunicacionPasarelaException("Respuesta perdida"); }).when(pasarela).reintegrarPago(anyString(), anyString(), any());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d));
        assertEquals(EstadoReintegro.RESULTADO_DESCONOCIDO, flujo.obtener(d.id()).reintegro().estado());
        assertThrows(IllegalArgumentException.class, () -> compras.crearCompra(nueva));
        reset(pasarela); completada(procesar(d)); assertNotNull(compras.crearCompra(nueva).getId());
    }
    @Test void reintegroRechazadoNoLiberaLimite() {
        var c = crear(true); compras.crearCompra(nuevaSolicitud(600L, 4)); var d = aprobada(c, List.of(c.getEntradasIds().get(0)));
        doReturn(new ResultadoPasarela(false, "rechazo")).when(pasarela).reintegrarPago(anyString(), anyString(), any());
        assertThrows(IllegalStateException.class, () -> procesar(d));
        var nueva = nuevaSolicitud(601L, 1); assertThrows(IllegalArgumentException.class, () -> compras.crearCompra(nueva));
    }
    @Test void reintegroConfirmadoLiberaLimiteAunqueLaAnulacionSigaPendiente() {
        var c = crear(true); compras.crearCompra(nuevaSolicitud(600L, 4)); var d = aprobada(c, List.of(c.getEntradasIds().get(0)));
        doThrow(new ComunicacionPasarelaException("Sin Entradas")).when(entradas).anularEntradas(any(), anyList());
        assertThrows(ComunicacionPasarelaException.class, () -> procesar(d));
        assertEquals(EstadoReintegro.COMPLETADO, flujo.obtener(d.id()).reintegro().estado());
        assertEquals(EstadoDevolucion.ERROR_PENDIENTE, servicio.obtener(d.id()).estado());
        assertNotNull(compras.crearCompra(nuevaSolicitud(601L, 1)).getId());
        reset(entradas); completada(procesar(d)); var excedida = nuevaSolicitud(602L, 1);
        assertThrows(IllegalArgumentException.class, () -> compras.crearCompra(excedida));
    }
    @Test void comprasConcurrentesSoloConsumenUnaVezElCupoDevuelto() throws Exception {
        var c = crear(true); compras.crearCompra(nuevaSolicitud(600L, 4)); completada(procesar(aprobada(c, List.of(c.getEntradasIds().get(0)))));
        var a = nuevaSolicitud(601L, 1); var b = nuevaSolicitud(602L, 1); var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (var dto : List.of(a, b)) resultados.add(pool.submit(() -> {
                assertTrue(inicio.await(10, TimeUnit.SECONDS));
                try { compras.crearCompra(dto); return true; } catch (IllegalArgumentException ex) { return false; }
            }));
            inicio.countDown(); int aceptadas = 0;
            for (var resultado : resultados) if (resultado.get(15, TimeUnit.SECONDS)) aceptadas++;
            assertEquals(1, aceptadas); assertEquals(3, compraRepository.count());
        }
    }
}
