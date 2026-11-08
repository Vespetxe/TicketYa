
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoDevolucionTest {

    @Test
    void permiteTransicionesValidas() {
        assertTrue(EstadoDevolucion.PENDIENTE
                .puedeTransicionarA(EstadoDevolucion.APROBADA));

        assertTrue(EstadoDevolucion.PENDIENTE
                .puedeTransicionarA(EstadoDevolucion.RECHAZADA));

        assertTrue(EstadoDevolucion.APROBADA
                .puedeTransicionarA(EstadoDevolucion.PROCESANDO));

        assertTrue(EstadoDevolucion.PROCESANDO
                .puedeTransicionarA(EstadoDevolucion.COMPLETADA));

        assertTrue(EstadoDevolucion.PROCESANDO
                .puedeTransicionarA(EstadoDevolucion.ERROR_PENDIENTE));

        assertTrue(EstadoDevolucion.ERROR_PENDIENTE
                .puedeTransicionarA(EstadoDevolucion.PROCESANDO));
    }

    @Test
    void rechazaTransicionesInvalidas() {
        assertFalse(EstadoDevolucion.PENDIENTE
                .puedeTransicionarA(EstadoDevolucion.COMPLETADA));

        assertFalse(EstadoDevolucion.RECHAZADA
                .puedeTransicionarA(EstadoDevolucion.APROBADA));

        assertFalse(EstadoDevolucion.COMPLETADA
                .puedeTransicionarA(EstadoDevolucion.PROCESANDO));

        assertFalse(EstadoDevolucion.APROBADA
                .puedeTransicionarA(EstadoDevolucion.RECHAZADA));

        assertFalse(EstadoDevolucion.PENDIENTE
                .puedeTransicionarA(null));
    }
}
