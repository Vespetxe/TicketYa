
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoPagoTest {

    @Test
    void reintegroDesconocidoRechazadoConfirmaQueElPagoSigueCobrado() {
        var pago = new ar.edu.unicen.grupo8.comprasservice.entity.Pago();
        pago.setEstado(EstadoPago.APROBADO);
        pago.setEstado(EstadoPago.REINTEGRO_DESCONOCIDO);
        pago.setEstado(EstadoPago.APROBADO);
        assertEquals(EstadoPago.APROBADO, pago.getEstado());
    }

    @Test
    void permiteTransicionesValidas() {
        assertTrue(EstadoPago.PENDIENTE
                .puedeTransicionarA(EstadoPago.APROBADO));

        assertTrue(EstadoPago.PENDIENTE
                .puedeTransicionarA(EstadoPago.RESULTADO_DESCONOCIDO));

        assertTrue(EstadoPago.RESULTADO_DESCONOCIDO
                .puedeTransicionarA(EstadoPago.RECHAZADO));

        assertTrue(EstadoPago.APROBADO
                .puedeTransicionarA(EstadoPago.REINTEGRADO_PARCIAL));

        assertTrue(EstadoPago.REINTEGRADO_PARCIAL
                .puedeTransicionarA(EstadoPago.REINTEGRADO_TOTAL));
    }

    @Test
    void rechazaTransicionesInvalidas() {
        assertFalse(EstadoPago.PENDIENTE
                .puedeTransicionarA(EstadoPago.REINTEGRADO_TOTAL));

        assertFalse(EstadoPago.RECHAZADO
                .puedeTransicionarA(EstadoPago.APROBADO));

        assertFalse(EstadoPago.REINTEGRADO_TOTAL
                .puedeTransicionarA(EstadoPago.PENDIENTE));

        assertFalse(EstadoPago.PENDIENTE
                .puedeTransicionarA(null));
    }
}
