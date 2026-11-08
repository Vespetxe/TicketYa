package ar.edu.unicen.grupo8.comprasservice.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DevolucionDTOTest {
    private boolean valido(Object dto) {
        try (var factory = Validation.buildDefaultValidatorFactory()) { return factory.getValidator().validate(dto).isEmpty(); }
    }

    @Test void solicitudValidaAceptaIdentificadoresYMotivo() {
        assertTrue(valido(new SolicitarDevolucionDTO(1L, 10L, List.of(100L), "No puedo asistir")));
    }
    @Test void solicitudRechazaIdentificadoresInvalidos() {
        for (Long id : Arrays.asList(null, 0L, -1L)) {
            assertFalse(valido(new SolicitarDevolucionDTO(id, 10L, List.of(100L), "Motivo")));
            assertFalse(valido(new SolicitarDevolucionDTO(1L, id, List.of(100L), "Motivo")));
            assertFalse(valido(new SolicitarDevolucionDTO(1L, 10L, Arrays.asList(id), "Motivo")));
        }
    }
    @Test void solicitudRechazaSeleccionNulaOVacia() {
        assertFalse(valido(new SolicitarDevolucionDTO(1L, 10L, null, "Motivo")));
        assertFalse(valido(new SolicitarDevolucionDTO(1L, 10L, List.of(), "Motivo")));
    }
    @Test void motivosObligatoriosYLimitadosEnLasTresOperaciones() {
        for (String motivo : Arrays.asList(null, "", "  ", "x".repeat(256))) {
            assertFalse(valido(new SolicitarDevolucionDTO(1L, 10L, List.of(100L), motivo)));
            assertFalse(valido(new AprobarDevolucionDTO(motivo)));
            assertFalse(valido(new RechazarDevolucionDTO(motivo)));
        }
        assertTrue(valido(new AprobarDevolucionDTO("Cumple condiciones")));
        assertTrue(valido(new RechazarDevolucionDTO("No corresponde")));
    }
}
