
package ar.edu.unicen.grupo8.comprasservice.client;

import ar.edu.unicen.grupo8.comprasservice.dto.ReservaDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservaClientSimuladoTest {

    private final ReservaClientSimulado reservaClient =
            new ReservaClientSimulado();

    @Test
    void debeConsultarReservaExistente() {

        ReservaDTO reserva = reservaClient.consultarReserva(1L);

        assertNotNull(reserva);
        assertEquals(1L, reserva.reservaId());
        assertEquals(10L, reserva.clienteId());
        assertEquals(20L, reserva.eventoId());
        assertEquals("TEMPORAL", reserva.estado());
        assertTrue(reserva.fechaVencimiento().isAfter(
                java.time.LocalDateTime.now(java.time.Clock.systemUTC())
        ));
    }


    @Test
    void debeConfirmarReservaTemporalVigente() {

        Long reservaId = 1L;

        // Consultamos el estado inicial
        ReservaDTO reservaInicial =
                reservaClient.consultarReserva(reservaId);

        assertEquals("TEMPORAL", reservaInicial.estado());

        // Confirmamos la reserva
        reservaClient.confirmarReserva(reservaId);

        // Consultamos nuevamente
        ReservaDTO reservaConfirmada =
                reservaClient.consultarReserva(reservaId);

        assertEquals("CONFIRMADA", reservaConfirmada.estado());
        assertEquals(reservaId, reservaConfirmada.reservaId());
    }


    @Test
    void debeLiberarReservaTemporal() {

        Long reservaId = 3L;

        // Consultamos el estado inicial
        ReservaDTO reservaInicial =
                reservaClient.consultarReserva(reservaId);

        assertEquals("TEMPORAL", reservaInicial.estado());

        // Liberamos la reserva
        reservaClient.liberarReserva(reservaId);

        // Consultamos nuevamente
        ReservaDTO reservaLiberada =
                reservaClient.consultarReserva(reservaId);

        assertEquals("LIBERADA", reservaLiberada.estado());
        assertEquals(reservaId, reservaLiberada.reservaId());
    }


    @Test
    void noDebeConfirmarReservaVencida() {

        Long reservaId = 2L;

        // Consultamos la reserva vencida
        ReservaDTO reserva = reservaClient.consultarReserva(reservaId);

        assertEquals("TEMPORAL", reserva.estado());

        assertTrue(
                reserva.fechaVencimiento().isBefore(
                        java.time.LocalDateTime.now(java.time.Clock.systemUTC())
                )
        );

        // Intentamos confirmar la reserva
        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> reservaClient.confirmarReserva(reservaId)
        );

        assertEquals(
                "La reserva se encuentra vencida",
                excepcion.getMessage()
        );

        // Verificamos que no haya cambiado de estado
        ReservaDTO reservaFinal = reservaClient.consultarReserva(reservaId);

        assertEquals("TEMPORAL", reservaFinal.estado());
    }


    @Test
    void debePermitirConfirmarReservaDosVeces() {

        Long reservaId = 1L;

        // Primera confirmación
        reservaClient.confirmarReserva(reservaId);

        ReservaDTO primeraConfirmacion =
                reservaClient.consultarReserva(reservaId);

        assertEquals("CONFIRMADA", primeraConfirmacion.estado());

        // Segunda confirmación
        assertDoesNotThrow(
                () -> reservaClient.confirmarReserva(reservaId)
        );

        ReservaDTO segundaConfirmacion =
                reservaClient.consultarReserva(reservaId);

        assertEquals("CONFIRMADA", segundaConfirmacion.estado());
        assertEquals(primeraConfirmacion, segundaConfirmacion);
    }


    @Test
    void debePermitirLiberarReservaDosVeces() {

        Long reservaId = 3L;

        // Primera liberación
        reservaClient.liberarReserva(reservaId);

        ReservaDTO primeraLiberacion =
                reservaClient.consultarReserva(reservaId);

        assertEquals("LIBERADA", primeraLiberacion.estado());

        // Segunda liberación
        assertDoesNotThrow(
                () -> reservaClient.liberarReserva(reservaId)
        );

        ReservaDTO segundaLiberacion =
                reservaClient.consultarReserva(reservaId);

        assertEquals("LIBERADA", segundaLiberacion.estado());
        assertEquals(primeraLiberacion, segundaLiberacion);
    }


    @Test
    void debeValidarReservaTemporalVigente() {

        Long reservaId = 1L;
        Long clienteId = 10L;
        Long eventoId = 20L;

        boolean resultado = reservaClient.reservaValida(
                reservaId,
                clienteId,
                eventoId
        );

        assertTrue(resultado);
    }


    @Test
    void debeRechazarReservaTemporalVencida() {

        Long reservaId = 2L;
        Long clienteId = 10L;
        Long eventoId = 20L;

        boolean resultado = reservaClient.reservaValida(
                reservaId,
                clienteId,
                eventoId
        );

        assertFalse(resultado);
    }


    @Test
    void debeRechazarReservaDeOtroCliente() {

        Long reservaId = 1L;
        Long clienteId = 99L;
        Long eventoId = 20L;

        boolean resultado = reservaClient.reservaValida(
                reservaId,
                clienteId,
                eventoId
        );

        assertFalse(resultado);
    }

    @Test
    void debeRechazarReservaDeOtroEvento() {

        Long reservaId = 1L;
        Long clienteId = 10L;
        Long eventoId = 99L;

        boolean resultado = reservaClient.reservaValida(
                reservaId,
                clienteId,
                eventoId
        );

        assertFalse(resultado);
    }


    @Test
    void debeRechazarReservaInexistente() {

        Long reservaId = 999L;
        Long clienteId = 10L;
        Long eventoId = 20L;

        boolean resultado = reservaClient.reservaValida(
                reservaId,
                clienteId,
                eventoId
        );

        assertFalse(resultado);
    }


    @Test
    void debeRechazarReservaYaConfirmada() {

        Long reservaId = 1L;
        Long clienteId = 10L;
        Long eventoId = 20L;

        // Confirmamos la reserva.
        reservaClient.confirmarReserva(reservaId);

        // Intentamos validarla nuevamente como temporal.
        boolean resultado = reservaClient.reservaValida(
                reservaId,
                clienteId,
                eventoId
        );

        assertFalse(resultado);

        // Comprobamos que conserva su estado confirmado.
        ReservaDTO reserva = reservaClient.consultarReserva(reservaId);

        assertEquals("CONFIRMADA", reserva.estado());
    }


    @Test
    void debeRechazarReservaLiberada() {

        Long reservaId = 1L;
        Long clienteId = 10L;
        Long eventoId = 20L;

        // Liberamos la reserva temporal.
        reservaClient.liberarReserva(reservaId);

        // Intentamos validarla nuevamente.
        boolean resultado = reservaClient.reservaValida(
                reservaId,
                clienteId,
                eventoId
        );

        assertFalse(resultado);

        // Comprobamos que conserva su estado liberado.
        ReservaDTO reserva = reservaClient.consultarReserva(reservaId);

        assertEquals("LIBERADA", reserva.estado());
    }











}
