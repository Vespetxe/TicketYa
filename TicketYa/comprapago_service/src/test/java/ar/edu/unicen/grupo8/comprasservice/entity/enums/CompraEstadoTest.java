
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompraEstadoTest {

    @Test
    void permiteTransicionesValidas() {
        Compra compra = new Compra();

        assertEquals(EstadoCompra.PENDIENTE, compra.getEstado());

        compra.setEstado(EstadoCompra.PROCESANDO);
        compra.setEstado(EstadoCompra.COMPLETADA);

        assertEquals(EstadoCompra.COMPLETADA, compra.getEstado());
    }

    @Test
    void rechazaTransicionesInvalidas() {
        Compra compra = new Compra();

        assertThrows(IllegalArgumentException.class,
                () -> compra.setEstado(EstadoCompra.COMPLETADA));

        assertEquals(EstadoCompra.PENDIENTE, compra.getEstado());
    }

    @Test
    void impideModificarUnaCompraCancelada() {
        Compra compra = new Compra();

        compra.setEstado(EstadoCompra.CANCELADA);

        assertThrows(IllegalArgumentException.class,
                () -> compra.setEstado(EstadoCompra.PROCESANDO));

        assertEquals(EstadoCompra.CANCELADA, compra.getEstado());
    }
}
