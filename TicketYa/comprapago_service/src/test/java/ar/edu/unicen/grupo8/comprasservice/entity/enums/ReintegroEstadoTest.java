
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

import ar.edu.unicen.grupo8.comprasservice.entity.Reintegro;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReintegroEstadoTest {

    @Test
    void permiteTransicionesValidas() {
        Reintegro reintegro = new Reintegro();

        assertEquals(EstadoReintegro.PENDIENTE, reintegro.getEstado());

        reintegro.setEstado(EstadoReintegro.RESULTADO_DESCONOCIDO);
        reintegro.setEstado(EstadoReintegro.COMPLETADO);

        assertEquals(EstadoReintegro.COMPLETADO, reintegro.getEstado());
    }

    @Test
    void rechazaTransicionesInvalidas() {
        Reintegro reintegro = new Reintegro();

        assertThrows(IllegalArgumentException.class,
                () -> reintegro.setEstado(EstadoReintegro.PENDIENTE));

        assertEquals(EstadoReintegro.PENDIENTE, reintegro.getEstado());
    }

    @Test
    void impideModificarUnReintegroCompletado() {
        Reintegro reintegro = new Reintegro();

        reintegro.setEstado(EstadoReintegro.COMPLETADO);

        assertThrows(IllegalArgumentException.class,
                () -> reintegro.setEstado(EstadoReintegro.RECHAZADO));

        assertEquals(EstadoReintegro.COMPLETADO, reintegro.getEstado());
    }

    @Test
    void impideModificarUnReintegroRechazado() {
        Reintegro reintegro = new Reintegro();

        reintegro.setEstado(EstadoReintegro.RECHAZADO);

        assertThrows(IllegalArgumentException.class,
                () -> reintegro.setEstado(EstadoReintegro.COMPLETADO));

        assertEquals(EstadoReintegro.RECHAZADO, reintegro.getEstado());
    }
}
