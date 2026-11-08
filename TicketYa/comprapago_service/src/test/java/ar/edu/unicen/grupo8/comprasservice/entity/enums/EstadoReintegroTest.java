
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoReintegroTest {

    @Test
    void permiteTransicionesValidas() {
        assertTrue(EstadoReintegro.PENDIENTE
                .puedeTransicionarA(EstadoReintegro.COMPLETADO));

        assertTrue(EstadoReintegro.PENDIENTE
                .puedeTransicionarA(EstadoReintegro.RECHAZADO));

        assertTrue(EstadoReintegro.PENDIENTE
                .puedeTransicionarA(EstadoReintegro.RESULTADO_DESCONOCIDO));

        assertTrue(EstadoReintegro.RESULTADO_DESCONOCIDO
                .puedeTransicionarA(EstadoReintegro.COMPLETADO));

        assertTrue(EstadoReintegro.RESULTADO_DESCONOCIDO
                .puedeTransicionarA(EstadoReintegro.RECHAZADO));
    }

    @Test
    void rechazaTransicionesInvalidas() {
        assertFalse(EstadoReintegro.PENDIENTE
                .puedeTransicionarA(EstadoReintegro.PENDIENTE));

        assertFalse(EstadoReintegro.COMPLETADO
                .puedeTransicionarA(EstadoReintegro.PENDIENTE));

        assertFalse(EstadoReintegro.RECHAZADO
                .puedeTransicionarA(EstadoReintegro.COMPLETADO));

        assertFalse(EstadoReintegro.PENDIENTE
                .puedeTransicionarA(null));
    }
}
