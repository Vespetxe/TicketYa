package ar.edu.unicen.grupo8.comprasservice.client;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class EventoClientSimuladoTest {
    private final EventoClientSimulado eventos = new EventoClientSimulado();

    @Test void fechaInicioEsUtcEstableYConservadaAlReconstruirCliente() {
        var almacen = new AlmacenSimulado.Memoria();
        var reloj = java.time.Clock.fixed(java.time.Instant.parse("2026-11-05T12:00:00Z"), java.time.ZoneOffset.UTC);
        var cliente = new EventoClientSimulado(almacen, reloj);
        assertTrue(cliente.eventoValido(20L));
        var fecha = cliente.obtenerFechaInicio(20L);
        assertEquals(java.time.LocalDateTime.of(2026, 11, 12, 12, 0), fecha);
        var reiniciado = new EventoClientSimulado(almacen, java.time.Clock.offset(reloj, java.time.Duration.ofDays(3)));
        assertEquals(fecha, reiniciado.obtenerFechaInicio(20L));
        assertThrows(IllegalArgumentException.class, () -> cliente.obtenerFechaInicio(999L));
    }

    @Test void aceptaEventosPublicadosDelCatalogo() {
        for (long id : new long[]{20, 21, 40, 200}) assertTrue(eventos.eventoValido(id));
        assertEquals(new BigDecimal("15000.00"), eventos.obtenerPrecioSector(20L, 1L));
        assertEquals(new BigDecimal("25000.00"), eventos.obtenerPrecioSector(20L, 2L));
        assertEquals(new BigDecimal("40000.00"), eventos.obtenerPrecioSector(20L, 3L));
    }

    @Test void rechazaNoPublicadoCanceladoFinalizadoEInexistente() {
        for (long id : new long[]{901, 902, 903, 999, 0, -1}) {
            assertFalse(eventos.eventoValido(id));
            assertThrows(IllegalArgumentException.class, () -> eventos.obtenerPrecioSector(id, 1L));
        }
        assertFalse(eventos.eventoValido(null));
    }

    @Test void noTruncaIdentificadoresLongDeSectores() {
        for (long id : new long[]{4294967297L, 4294967298L, 4294967299L, Long.MAX_VALUE, 0, -1})
            assertThrows(IllegalArgumentException.class, () -> eventos.obtenerPrecioSector(20L, id));
        assertThrows(IllegalArgumentException.class, () -> eventos.obtenerPrecioSector(20L, null));
    }
}
