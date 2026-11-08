package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import ar.edu.unicen.grupo8.comprasservice.exception.*;
import ar.edu.unicen.grupo8.comprasservice.repository.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PagoServiceTest {
    private Compra compra;
    private ReservaClientSimulado reservas;
    private EntradaClientSimulado entradas;
    private PasarelaPagoSimuladaService pasarela;
    private PagoRepository pagos;
    private PagoService servicio;
    private final Map<Long, Pago> guardados = new HashMap<>();

    @BeforeEach
    void setUp() {
        compra = new Compra(); compra.setId(1L); compra.setClienteId(10L); compra.setEventoId(20L); compra.setReservaId(1L);
        compra.setImporteTotal(new BigDecimal("50000.00"));
        DetalleCompra d = new DetalleCompra(); d.setSectorId(1L); d.setAsientoId(1L); d.setCantidad(1); compra.agregarDetalle(d);
        reservas = spy(new ReservaClientSimulado()); entradas = spy(new EntradaClientSimulado());
        reservas.registrarReserva(new ReservaDTO(1L, 10L, 20L, "TEMPORAL", LocalDateTime.now(java.time.Clock.systemUTC()).plusMinutes(15),
                List.of(new DetalleEmisionDTO(1L, 1L, 1))));
        pasarela = spy(new PasarelaPagoSimuladaService());
        CompraRepository compras = mock(CompraRepository.class); pagos = mock(PagoRepository.class);
        ReintegroRepository reintegros = mock(ReintegroRepository.class);
        Map<String, Reintegro> reembolsos = new HashMap<>();
        when(compras.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(compra));
        when(pagos.findByClaveIdempotencia(anyString())).thenAnswer(i -> guardados.values().stream()
                .filter(p -> p.getClaveIdempotencia().equals(i.getArgument(0))).findFirst());
        when(pagos.findByCompraId(1L)).thenAnswer(i -> new ArrayList<>(guardados.values()));
        when(pagos.saveAndFlush(any())).thenAnswer(i -> {
            Pago p = i.getArgument(0); p.setId((long) guardados.size() + 1); guardados.put(p.getId(), p); return p;
        });
        when(pagos.findById(anyLong())).thenAnswer(i -> Optional.ofNullable(guardados.get(i.getArgument(0))));
        when(reintegros.existsByClaveIdempotencia(anyString())).thenAnswer(i -> reembolsos.containsKey(i.getArgument(0)));
        when(reintegros.findByClaveIdempotencia(anyString())).thenAnswer(i -> Optional.ofNullable(reembolsos.get(i.getArgument(0))));
        when(reintegros.save(any())).thenAnswer(i -> { Reintegro r = i.getArgument(0); reembolsos.put(r.getClaveIdempotencia(), r); return r; });
        CompraService consultas = mock(CompraService.class);
        when(consultas.obtenerCompraPorId(1L)).thenReturn(compra);
        servicio = new PagoService(reservas, entradas, pasarela,
                new PagoRegistroService(compras, pagos, reintegros, Clock.systemUTC()), consultas, pagos, Clock.systemUTC());
    }

    @Test void debePermitirCompraPendienteConReservaVigente() { assertDoesNotThrow(() -> servicio.validarCondicionesPrevias(compra)); }
    @Test void debeRechazarCompraCancelada() { compra.setEstado(EstadoCompra.CANCELADA); assertThrows(IllegalArgumentException.class, () -> servicio.validarCondicionesPrevias(compra)); }
    @Test void debeRechazarCompraCompletada() { compra.setEstado(EstadoCompra.PROCESANDO); compra.setEstado(EstadoCompra.COMPLETADA); assertThrows(IllegalArgumentException.class, () -> servicio.validarCondicionesPrevias(compra)); }
    @Test void debeRechazarCompraEnProcesamiento() { compra.setEstado(EstadoCompra.PROCESANDO); assertThrows(IllegalArgumentException.class, () -> servicio.validarCondicionesPrevias(compra)); }
    @Test void debeRechazarReservaVencida() { doReturn(false).when(reservas).reservaValida(1L, 10L, 20L); assertThrows(IllegalArgumentException.class, () -> servicio.validarCondicionesPrevias(compra)); }
    @Test void debeRechazarReservaIncompatible() { compra.getDetalles().get(0).setSectorId(2L); assertThrows(IllegalArgumentException.class, () -> servicio.validarCondicionesPrevias(compra)); }
    @Test void debeRechazarCompraNula() { assertThrows(IllegalArgumentException.class, () -> servicio.validarCondicionesPrevias(null)); }

    @Test void procesarPagoAprobado() {
        Pago p = servicio.procesarPago(1L, "aprobado", MedioPago.TARJETA_DEBITO);
        assertEquals(EstadoPago.APROBADO, p.getEstado()); assertEquals(compra.getImporteTotal(), p.getImporte());
        assertNotNull(p.getFechaCreacion()); assertNotNull(p.getFechaProcesamiento()); assertNotNull(p.getReferenciaExterna());
        assertEquals(EstadoCompra.COMPLETADA, compra.getEstado()); assertEquals(1, compra.getEntradasIds().size());
        var orden = inOrder(reservas, entradas);
        orden.verify(reservas).confirmarReserva(1L);
        orden.verify(entradas).solicitarEntradas(eq(1L), eq(20L), eq(10L), anyList());
    }
    @Test void procesarPagoRechazado() {
        compra.setImporteTotal(new BigDecimal("150000"));
        Pago p = servicio.procesarPago(1L, "rechazado", MedioPago.TARJETA_DEBITO);
        assertEquals(EstadoPago.RECHAZADO, p.getEstado()); assertNotNull(p.getFechaProcesamiento());
        assertEquals(EstadoCompra.FALLIDA, compra.getEstado()); assertEquals("LIBERADA", reservas.consultarReserva(1L).estado());
        verify(reservas, never()).confirmarReserva(any()); verify(entradas, never()).solicitarEntradas(any(), any(), any(), anyList());
    }
    @Test void noDebeProcesarDosVecesLaMismaClave() {
        Pago p = servicio.procesarPago(1L, "repetida", MedioPago.TARJETA_DEBITO);
        assertSame(p, servicio.procesarPago(1L, "repetida", MedioPago.TARJETA_DEBITO));
        verify(pasarela, times(1)).procesarPago(any(), any()); verify(reservas, times(1)).confirmarReserva(any());
        verify(entradas, times(1)).solicitarEntradas(any(), any(), any(), anyList());
    }
    @Test void debeRechazarClaveIdempotenciaDeOtraCompra() {
        Pago ajeno = new Pago(); Compra otra = new Compra(); otra.setId(2L); ajeno.setCompra(otra);
        when(pagos.findByClaveIdempotencia("ajena")).thenReturn(Optional.of(ajeno));
        assertThrows(IllegalStateException.class, () -> servicio.procesarPago(1L, "ajena", MedioPago.TARJETA_DEBITO)); verify(pasarela, never()).procesarPago(any(), any());
    }
    @Test void debeRechazarReintentoDePagoPendiente() {
        compra.setEjecutor("otra-solicitud"); compra.setBloqueoHasta(LocalDateTime.now(java.time.Clock.systemUTC()).plusMinutes(1));
        assertThrows(IllegalStateException.class, () -> servicio.procesarPago(1L, "pendiente", MedioPago.TARJETA_DEBITO));
        verify(pasarela, never()).procesarPago(any(), any());
    }
    @Test void debeRecuperarPagoConResultadoDesconocido() {
        Pago p = servicio.procesarPago(1L, "simular-timeout", MedioPago.TARJETA_DEBITO); assertEquals(EstadoPago.RESULTADO_DESCONOCIDO, p.getEstado());
        var recuperado = servicio.procesarPagoConResultado(1L, "simular-timeout", MedioPago.TARJETA_DEBITO);
        assertFalse(recuperado.creado()); assertEquals(EstadoPago.APROBADO, recuperado.pago().getEstado());
        assertEquals(EstadoCompra.COMPLETADA, compra.getEstado()); verify(pasarela, times(1)).procesarPago(any(), any());
    }
    @Test void noDebeProcesarCompraConPagoYaAprobado() {
        servicio.procesarPago(1L, "primera", MedioPago.TARJETA_DEBITO);
        assertThrows(IllegalStateException.class, () -> servicio.procesarPago(1L, "otra", MedioPago.TARJETA_DEBITO));
        verify(pasarela, times(1)).procesarPago(any(), any());
    }
    @Test void debePermitirReintentoDespuesDePagoRechazado() {
        // Reintentar la misma solicitud devuelve el rechazo; la reserva liberada requiere una nueva compra.
        compra.setImporteTotal(new BigDecimal("150000"));
        Pago p = servicio.procesarPago(1L, "rechazo", MedioPago.TARJETA_DEBITO);
        assertSame(p, servicio.procesarPago(1L, "rechazo", MedioPago.TARJETA_DEBITO));
        assertThrows(IllegalStateException.class, () -> servicio.procesarPago(1L, "nuevo-cobro", MedioPago.TARJETA_DEBITO));
        verify(pasarela, times(1)).procesarPago(any(), any());
    }
    @Test void debeRegistrarPagoConResultadoDesconocido() {
        Pago p = servicio.procesarPago(1L, "simular-timeout", MedioPago.TARJETA_DEBITO);
        assertEquals(EstadoPago.RESULTADO_DESCONOCIDO, p.getEstado()); assertNull(p.getReferenciaExterna());
        assertEquals(EstadoCompra.PROCESANDO, compra.getEstado()); assertEquals(1, guardados.size());
        assertThrows(IllegalStateException.class, () -> servicio.procesarPago(1L, "otra", MedioPago.TARJETA_DEBITO));
        verify(reservas, never()).confirmarReserva(any());
    }
    @Test void debeObtenerPagoPorId() { Pago p = servicio.procesarPago(1L, "consulta", MedioPago.TARJETA_DEBITO); assertSame(p, servicio.obtenerPagoPorId(p.getId())); }
    @Test void debeObtenerPagosPorCompra() { Pago p = servicio.procesarPago(1L, "lista", MedioPago.TARJETA_DEBITO); assertEquals(List.of(p), servicio.obtenerPagosPorCompra(1L)); }
    @Test void debeDetectarFallaAlConfirmarReservaDespuesDePagoAprobado() {
        doThrow(new IllegalStateException("Sin respuesta")).when(reservas).confirmarReserva(1L);
        Pago p = servicio.procesarPago(1L, "falla", MedioPago.TARJETA_DEBITO);
        assertEquals(EstadoPago.APROBADO, p.getEstado()); assertEquals(EtapaCompra.CONFIRMACION, compra.getEtapa());
        assertEquals(EstadoCompra.PROCESANDO, compra.getEstado()); verify(pasarela, never()).reintegrarPago(any(), any());
        verify(entradas, never()).solicitarEntradas(any(), any(), any(), anyList());
    }
    @Test void noDebeEmitirEntradasSiReservaNoEstaConfirmada() {
        doNothing().when(reservas).confirmarReserva(1L);
        servicio.procesarPago(1L, "sin-confirmar", MedioPago.TARJETA_DEBITO); assertEquals(EtapaCompra.CONFIRMACION, compra.getEtapa());
        verify(entradas, never()).solicitarEntradas(any(), any(), any(), anyList());
    }
    @Test void debeCompensarRechazoDefinitivoDeReserva() {
        doThrow(new OperacionExternaRechazadaException("Vencida")).when(reservas).confirmarReserva(1L);
        Pago p = servicio.procesarPago(1L, "compensar", MedioPago.TARJETA_DEBITO);
        assertEquals(EstadoPago.REINTEGRADO_TOTAL, p.getEstado()); assertEquals(p.getImporte(), p.getImporteReintegrado());
        assertEquals(EstadoCompra.FALLIDA, compra.getEstado()); assertEquals("LIBERADA", reservas.consultarReserva(1L).estado());
        servicio.procesarPago(1L, "compensar", MedioPago.TARJETA_DEBITO); verify(pasarela, times(1)).reintegrarPago(any(), any());
    }
    @Test void debeRecuperarEmisionConRespuestaPerdida() {
        doAnswer(i -> { i.callRealMethod(); throw new IllegalStateException("Respuesta perdida"); })
                .when(entradas).solicitarEntradas(any(), any(), any(), anyList());
        servicio.procesarPago(1L, "emision", MedioPago.TARJETA_DEBITO); assertEquals(EtapaCompra.EMISION, compra.getEtapa());
        servicio.procesarPago(1L, "emision", MedioPago.TARJETA_DEBITO); assertEquals(EstadoCompra.COMPLETADA, compra.getEstado());
        verify(entradas, times(1)).solicitarEntradas(any(), any(), any(), anyList()); verify(pasarela, never()).reintegrarPago(any(), any());
    }
    @Test void debeRecuperarReintegroConRespuestaPerdida() {
        doThrow(new OperacionExternaRechazadaException("Sin emision")).when(entradas).solicitarEntradas(any(), any(), any(), anyList());
        doAnswer(i -> { i.callRealMethod(); throw new IllegalStateException("Respuesta perdida"); }).when(pasarela).reintegrarPago(any(), any());
        Pago p = servicio.procesarPago(1L, "reintegro", MedioPago.TARJETA_DEBITO); assertEquals(EstadoPago.REINTEGRO_DESCONOCIDO, p.getEstado());
        p = servicio.procesarPago(1L, "reintegro", MedioPago.TARJETA_DEBITO); assertEquals(EstadoPago.REINTEGRADO_TOTAL, p.getEstado());
        assertEquals(EstadoCompra.FALLIDA, compra.getEstado()); verify(pasarela, times(1)).reintegrarPago(any(), any());
    }

    @Test void medioAusenteParaPagoNuevoSeRechazaAntesDeConsultarReservaOCobrar() {
        assertThrows(IllegalArgumentException.class, () -> servicio.procesarPago(1L, "sin-medio", null));
        verify(reservas, never()).reservaValida(any(), any(), any()); verify(pasarela, never()).procesarPago(any(), any());
        assertTrue(guardados.isEmpty());
    }
    @Test void medioFormaParteDeLaIdempotenciaInclusoConResultadoDesconocido() {
        var p = servicio.procesarPago(1L, "simular-timeout", MedioPago.TARJETA_CREDITO);
        assertEquals(MedioPago.TARJETA_CREDITO, p.getMedioPago());
        assertThrows(IllegalStateException.class, () -> servicio.procesarPago(1L, "simular-timeout", MedioPago.TRANSFERENCIA));
        assertEquals(EstadoPago.RESULTADO_DESCONOCIDO, p.getEstado());
        var recuperado = servicio.procesarPago(1L, "simular-timeout", MedioPago.TARJETA_CREDITO);
        assertEquals(MedioPago.TARJETA_CREDITO, recuperado.getMedioPago()); assertEquals(EstadoPago.APROBADO, recuperado.getEstado());
        verify(pasarela, times(1)).procesarPago(any(), any());
    }
}
