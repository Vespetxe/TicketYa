package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DevolucionServiceTest {
    private CompraService compras;
    private EntradaClient entradas;
    private EventoClient eventos;
    private DevolucionRegistroService registro;
    private DevolucionService servicio;
    private Compra compra;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneOffset.UTC);
    private SolicitarDevolucionDTO solicitud() { return new SolicitarDevolucionDTO(1L, 10L, List.of(100L), "Solicitud"); }

    @BeforeEach void preparar() {
        compras = mock(CompraService.class); entradas = mock(EntradaClient.class); eventos = mock(EventoClient.class);
        registro = mock(DevolucionRegistroService.class); servicio = new DevolucionService(compras, entradas, eventos, registro, clock);
        compra = new Compra(); compra.setId(1L); compra.setClienteId(10L); compra.setEventoId(20L);
        compra.setEstado(EstadoCompra.PROCESANDO); compra.setEstado(EstadoCompra.COMPLETADA);
        when(compras.obtenerCompraPorId(1L)).thenReturn(compra);
    }

    @Test void capturaFechaUtcYConsultaSoloLecturasExternas() {
        var tickets = List.of(new EntradaDTO(100L, 1L, 20L, 10L, 1L, null, "EMITIDA", "QR"));
        var inicio = LocalDateTime.now(clock).plusDays(7);
        when(entradas.consultarEntradas(1L)).thenReturn(tickets); when(eventos.obtenerFechaInicio(20L)).thenReturn(inicio);
        servicio.solicitar(solicitud());
        verify(registro).registrar(solicitud(), tickets, inicio, LocalDateTime.now(clock));
        verify(entradas, never()).anularEntradas(any()); verify(entradas, never()).solicitarEntradas(any(), any(), any(), any());
        verify(eventos, never()).obtenerPrecioSector(any(), any());
    }
    @Test void titularidadSeRechazaAntesDeConsultarServiciosExternos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.solicitar(new SolicitarDevolucionDTO(1L, 99L, List.of(100L), "Motivo")));
        verifyNoInteractions(entradas, eventos, registro);
    }
    @Test void comprasNoCompletadasNoPermitenSolicitar() {
        for (EstadoCompra estado : List.of(EstadoCompra.PENDIENTE, EstadoCompra.PROCESANDO, EstadoCompra.CANCELADA, EstadoCompra.FALLIDA)) {
            Compra otra = mock(Compra.class); when(otra.getClienteId()).thenReturn(10L); when(otra.getEstado()).thenReturn(estado);
            when(compras.obtenerCompraPorId(1L)).thenReturn(otra);
            assertThrows(IllegalStateException.class, () -> servicio.solicitar(solicitud()));
        }
        verifyNoInteractions(entradas, eventos, registro);
    }
    @Test void duplicadosEnLaMismaSolicitudSeRechazanAntesDeConsultar() {
        assertThrows(IllegalArgumentException.class, () -> servicio.solicitar(new SolicitarDevolucionDTO(1L, 10L, List.of(100L, 100L), "Motivo")));
        verifyNoInteractions(compras, entradas, eventos, registro);
    }
    @Test void decisionesDeleganEstadoYMotivoSinEfectosExternos() {
        servicio.aprobar(1L, new AprobarDevolucionDTO("Aprobada por condiciones"));
        servicio.rechazar(2L, new RechazarDevolucionDTO("Rechazada por condiciones"));
        verify(registro).decidir(1L, EstadoDevolucion.APROBADA, "Aprobada por condiciones");
        verify(registro).decidir(2L, EstadoDevolucion.RECHAZADA, "Rechazada por condiciones");
        verifyNoInteractions(compras, entradas, eventos);
    }
}
