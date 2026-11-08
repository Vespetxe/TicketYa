
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import ar.edu.unicen.grupo8.comprasservice.entity.Pago;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PagoEstadoTest {

    @Test
    void permiteTransicionesValidas() {
        Pago pago = new Pago();

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());

        pago.setEstado(EstadoPago.APROBADO);
        pago.setEstado(EstadoPago.REINTEGRADO_PARCIAL);
        pago.setEstado(EstadoPago.REINTEGRADO_TOTAL);

        assertEquals(EstadoPago.REINTEGRADO_TOTAL, pago.getEstado());
    }

    @Test
    void rechazaTransicionesInvalidas() {
        Pago pago = new Pago();

        assertThrows(IllegalArgumentException.class,
                () -> pago.setEstado(EstadoPago.REINTEGRADO_TOTAL));

        assertEquals(EstadoPago.PENDIENTE, pago.getEstado());
    }

    @Test
    void impideModificarUnPagoRechazado() {
        Pago pago = new Pago();

        pago.setEstado(EstadoPago.RECHAZADO);

        assertThrows(IllegalArgumentException.class,
                () -> pago.setEstado(EstadoPago.APROBADO));

        assertEquals(EstadoPago.RECHAZADO, pago.getEstado());
    }
}
