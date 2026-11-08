
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import ar.edu.unicen.grupo8.comprasservice.entity.Devolucion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DevolucionEstadoTest {

    @Test
    void permiteTransicionesValidas() {
        Devolucion devolucion = new Devolucion();

        assertEquals(EstadoDevolucion.PENDIENTE, devolucion.getEstado());

        devolucion.setEstado(EstadoDevolucion.APROBADA);
        devolucion.setEstado(EstadoDevolucion.PROCESANDO);
        devolucion.setEstado(EstadoDevolucion.COMPLETADA);

        assertEquals(EstadoDevolucion.COMPLETADA, devolucion.getEstado());
    }

    @Test
    void rechazaTransicionesInvalidas() {
        Devolucion devolucion = new Devolucion();

        assertThrows(IllegalArgumentException.class,
                () -> devolucion.setEstado(EstadoDevolucion.COMPLETADA));

        assertEquals(EstadoDevolucion.PENDIENTE, devolucion.getEstado());
    }

    @Test
    void impideModificarUnaDevolucionRechazada() {
        Devolucion devolucion = new Devolucion();

        devolucion.setEstado(EstadoDevolucion.RECHAZADA);

        assertThrows(IllegalArgumentException.class,
                () -> devolucion.setEstado(EstadoDevolucion.APROBADA));

        assertEquals(EstadoDevolucion.RECHAZADA, devolucion.getEstado());
    }

    @Test
    void permiteReprocesarUnaDevolucionConError() {
        Devolucion devolucion = new Devolucion();

        devolucion.setEstado(EstadoDevolucion.APROBADA);
        devolucion.setEstado(EstadoDevolucion.PROCESANDO);
        devolucion.setEstado(EstadoDevolucion.ERROR_PENDIENTE);
        devolucion.setEstado(EstadoDevolucion.PROCESANDO);

        assertEquals(EstadoDevolucion.PROCESANDO, devolucion.getEstado());
    }
}
