package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.client.*;
import ar.edu.unicen.grupo8.comprasservice.dto.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ReintegrosSimuladosTest {
    private final AlmacenSimulado almacen = new AlmacenSimulado.Memoria();
    private final PasarelaPagoSimuladaService pasarela = new PasarelaPagoSimuladaService(almacen);
    @Test void variosReintegrosConservanReferenciaYTopeAgregado() {
        pasarela.procesarPago("pago", new BigDecimal("100"));
        var a = pasarela.reintegrarPago("pago", "a", new BigDecimal("30"));
        var repetida = pasarela.reintegrarPago("pago", "a", new BigDecimal("30.00"));
        assertEquals(a.getReferenciaExterna(), repetida.getReferenciaExterna());
        assertTrue(pasarela.reintegrarPago("pago", "b", new BigDecimal("70")).isAprobado());
        assertThrows(IllegalArgumentException.class, () -> pasarela.reintegrarPago("pago", "c", BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> pasarela.reintegrarPago("pago", "a", new BigDecimal("31")));
        assertNotNull(new PasarelaPagoSimuladaService(almacen).consultarReintegro("pago", "a"));
    }
    @Test void claveGlobalNoPuedeVincularseAOtroPago() {
        pasarela.procesarPago("uno", BigDecimal.TEN); pasarela.procesarPago("dos", BigDecimal.TEN);
        pasarela.reintegrarPago("uno", "operacion", BigDecimal.ONE);
        assertThrows(IllegalStateException.class, () -> pasarela.reintegrarPago("dos", "operacion", BigDecimal.ONE));
        assertThrows(IllegalStateException.class, () -> pasarela.consultarReintegro("dos", "operacion"));
    }
    @Test void compensacionAnteriorYReintegroNuevoCompartenTope() {
        pasarela.procesarPago("pago", BigDecimal.TEN); var legado = pasarela.reintegrarPago("pago", new BigDecimal("4"));
        assertEquals(legado.getReferenciaExterna(), pasarela.consultarReintegro("pago").getReferenciaExterna());
        pasarela.reintegrarPago("pago", "otra", new BigDecimal("6"));
        assertThrows(IllegalArgumentException.class, () -> pasarela.reintegrarPago("pago", "exceso", BigDecimal.ONE));
    }
    @Test void reconoceDiarioLegadoPersistido() {
        pasarela.procesarPago("pago", BigDecimal.TEN);
        almacen.actualizar("reintegro:pago", PasarelaPagoSimuladaService.Operacion.class,
                v -> new PasarelaPagoSimuladaService.Operacion(BigDecimal.TEN, true, "anterior"));
        assertEquals("anterior", pasarela.consultarReintegro("pago").getReferenciaExterna());
        assertThrows(IllegalArgumentException.class, () -> pasarela.reintegrarPago("pago", "nuevo", BigDecimal.ONE));
    }
    @Test void validaImportesYPagoAprobadoAntesDelEfecto() {
        assertThrows(IllegalStateException.class, () -> pasarela.reintegrarPago("sin-pago", "x", BigDecimal.ONE));
        pasarela.procesarPago("rechazado", new BigDecimal("100001"));
        assertThrows(IllegalStateException.class, () -> pasarela.reintegrarPago("rechazado", "x", BigDecimal.ONE));
        pasarela.procesarPago("pago", BigDecimal.TEN);
        for (var importe : List.of(BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("0.001")))
            assertThrows(IllegalArgumentException.class, () -> pasarela.reintegrarPago("pago", "x", importe));
        assertThrows(IllegalArgumentException.class, () -> pasarela.reintegrarPago("pago", " ", BigDecimal.ONE));
    }
    @Test void concurrenciaDelSimuladorNoSuperaElPago() throws Exception {
        pasarela.procesarPago("pago", BigDecimal.TEN);
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (String clave : List.of("a", "b")) resultados.add(pool.submit(() -> {
                assertTrue(inicio.await(5, TimeUnit.SECONDS));
                try { pasarela.reintegrarPago("pago", clave, new BigDecimal("6")); return true; }
                catch (IllegalArgumentException ex) { return false; }
            }));
            inicio.countDown(); int aprobados = 0;
            for (var resultado : resultados) if (resultado.get(5, TimeUnit.SECONDS)) aprobados++;
            assertEquals(1, aprobados);
        }
    }
    @Test void anulacionSeleccionadaNoAfectaLasOtrasEntradasYEsDurable() {
        var entradas = new EntradaClientSimulado(almacen);
        var emitidas = entradas.solicitarEntradas(1L, 20L, 10L, List.of(new DetalleEmisionDTO(1L, 101L, 1), new DetalleEmisionDTO(1L, 102L, 1)));
        entradas.anularEntradas(1L, List.of(emitidas.get(0).entradaId())); entradas.anularEntradas(1L, List.of(emitidas.get(0).entradaId()));
        assertEquals(List.of("ANULADA", "EMITIDA"), new EntradaClientSimulado(almacen).consultarEntradas(1L).stream().map(EntradaDTO::estado).toList());
        assertThrows(IllegalStateException.class, () -> entradas.anularEntradas(1L, List.of(999L)));
    }
    @Test void liberacionNumeradaEsIdempotenteYNoLiberaOtroAsiento() {
        var reservas = new ReservaClientSimulado(almacen, Clock.systemUTC());
        reservas.registrarReserva(new ReservaDTO(600L, 10L, 20L, "CONFIRMADA", LocalDateTime.now(Clock.systemUTC()),
                List.of(new DetalleEmisionDTO(1L, 101L, 1), new DetalleEmisionDTO(1L, 102L, 1))));
        var ticket = new EntradaDTO(1L, 1L, 20L, 10L, 1L, 101L, "ANULADA", "uno");
        reservas.liberarLugares(600L, List.of(ticket)); reservas.liberarLugares(600L, List.of(ticket));
        assertEquals(List.of(1L), new ReservaClientSimulado(almacen, Clock.systemUTC()).consultarLugaresLiberados(600L));
        assertThrows(IllegalStateException.class, () -> reservas.liberarLugares(600L,
                List.of(new EntradaDTO(2L, 1L, 20L, 10L, 1L, 103L, "ANULADA", "dos"))));
        assertThrows(IllegalArgumentException.class, () -> reservas.liberarLugares(600L,
                List.of(new EntradaDTO(3L, 1L, 20L, 10L, 1L, 102L, "EMITIDA", "tres"))));
    }
    @Test void cancelacionSimuladaEsDurableYBloqueaNuevasCompras() {
        var eventos = new EventoClientSimulado(almacen, Clock.systemUTC()); assertTrue(eventos.eventoValido(20L));
        var fecha = eventos.obtenerFechaInicio(20L); eventos.registrarCancelacion(20L); eventos.registrarCancelacion(20L);
        var nuevo = new EventoClientSimulado(almacen, Clock.systemUTC()); assertTrue(nuevo.eventoCancelado(20L));
        assertFalse(nuevo.eventoValido(20L)); assertEquals(fecha, nuevo.obtenerFechaInicio(20L));
    }
}
