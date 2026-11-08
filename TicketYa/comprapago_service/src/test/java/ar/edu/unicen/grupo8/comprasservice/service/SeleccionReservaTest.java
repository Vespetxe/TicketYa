package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SeleccionReservaTest {
    @Test void comparaCantidadesAgrupadasSinDependerDelOrden() {
        var reserva = new ReservaDTO(1L, 10L, 20L, "TEMPORAL", null,
                List.of(new DetalleEmisionDTO(1L, null, 2), new DetalleEmisionDTO(2L, 99L, 1)));
        assertDoesNotThrow(() -> SeleccionReserva.validar(reserva,
                List.of(new DetalleEmisionDTO(2L, 99L, 1), new DetalleEmisionDTO(1L, null, 1), new DetalleEmisionDTO(1L, null, 1))));
    }
    @Test void rechazaCantidadSectorYAsientoDiferentes() {
        var reserva = new ReservaDTO(1L, 10L, 20L, "TEMPORAL", null, List.of(new DetalleEmisionDTO(1L, 99L, 1)));
        for (var detalle : List.of(new DetalleEmisionDTO(1L, null, 1), new DetalleEmisionDTO(2L, 99L, 1), new DetalleEmisionDTO(1L, 98L, 1)))
            assertThrows(IllegalArgumentException.class, () -> SeleccionReserva.validar(reserva, List.of(detalle)));
    }
    @Test void noPermiteMultiplesEntradasParaUnAsiento() {
        assertThrows(IllegalArgumentException.class, () -> SeleccionReserva.agrupar(List.of(new DetalleEmisionDTO(1L, 99L, 2))));
        assertThrows(IllegalArgumentException.class, () -> SeleccionReserva.agrupar(List.of(new DetalleEmisionDTO(1L, 99L, 1), new DetalleEmisionDTO(1L, 99L, 1))));
    }
}
