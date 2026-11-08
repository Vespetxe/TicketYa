package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoCompraTest {

    @Test
    void permiteTransicionesValidas() {

        assertTrue(EstadoCompra.PENDIENTE
                .puedeTransicionarA(EstadoCompra.PROCESANDO));

        assertTrue(EstadoCompra.PENDIENTE
                .puedeTransicionarA(EstadoCompra.CANCELADA));

        assertTrue(EstadoCompra.PENDIENTE
                .puedeTransicionarA(EstadoCompra.FALLIDA));

        assertTrue(EstadoCompra.PROCESANDO
                .puedeTransicionarA(EstadoCompra.COMPLETADA));

        assertTrue(EstadoCompra.PROCESANDO
                .puedeTransicionarA(EstadoCompra.FALLIDA));

        assertTrue(EstadoCompra.PROCESANDO
                .puedeTransicionarA(EstadoCompra.CANCELADA));

        assertTrue(EstadoCompra.COMPLETADA
                .puedeTransicionarA(EstadoCompra.DEVUELTA_PARCIAL));

        assertTrue(EstadoCompra.COMPLETADA
                .puedeTransicionarA(EstadoCompra.DEVUELTA_TOTAL));

        assertTrue(EstadoCompra.COMPLETADA
                .puedeTransicionarA(EstadoCompra.CANCELADA));

        assertTrue(EstadoCompra.DEVUELTA_PARCIAL
                .puedeTransicionarA(EstadoCompra.DEVUELTA_TOTAL));
    }

    @Test
    void rechazaTransicionesInvalidas() {

        assertFalse(EstadoCompra.PENDIENTE
                .puedeTransicionarA(EstadoCompra.COMPLETADA));

        assertFalse(EstadoCompra.PROCESANDO
                .puedeTransicionarA(EstadoCompra.DEVUELTA_TOTAL));

        assertFalse(EstadoCompra.CANCELADA
                .puedeTransicionarA(EstadoCompra.COMPLETADA));

        assertFalse(EstadoCompra.FALLIDA
                .puedeTransicionarA(EstadoCompra.PROCESANDO));

        assertFalse(EstadoCompra.DEVUELTA_TOTAL
                .puedeTransicionarA(EstadoCompra.PENDIENTE));

        assertFalse(EstadoCompra.DEVUELTA_PARCIAL
                .puedeTransicionarA(EstadoCompra.COMPLETADA));

        assertFalse(EstadoCompra.PENDIENTE
                .puedeTransicionarA(null));
    }

    @Test
    void estadosTerminalesNoPermitenNuevasTransiciones() {

        for (EstadoCompra estado : new EstadoCompra[]{
                EstadoCompra.FALLIDA,
                EstadoCompra.CANCELADA,
                EstadoCompra.DEVUELTA_TOTAL
        }) {
            for (EstadoCompra destino : EstadoCompra.values()) {
                assertFalse(
                        estado.puedeTransicionarA(destino),
                        estado + " no debería permitir transición a " + destino
                );
            }
        }
    }
}