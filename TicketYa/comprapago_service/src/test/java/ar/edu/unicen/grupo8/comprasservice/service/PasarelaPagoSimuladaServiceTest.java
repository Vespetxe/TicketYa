
package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.dto.ResultadoPasarela;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PasarelaPagoSimuladaServiceTest {

    private PasarelaPagoSimuladaService pasarela;

    @BeforeEach
    void setUp() {
        pasarela = new PasarelaPagoSimuladaService();
    }

    @Test
    void debeAprobarPagoConImporteMenorAlLimite() {
        ResultadoPasarela resultado = pasarela.procesarPago(
                "clave-001",
                new BigDecimal("50000")
        );

        assertTrue(resultado.isAprobado());
        assertNotNull(resultado.getReferenciaExterna());
    }

    @Test
    void debeAprobarPagoConImporteIgualAlLimite() {
        ResultadoPasarela resultado = pasarela.procesarPago(
                "clave-002",
                new BigDecimal("100000")
        );

        assertTrue(resultado.isAprobado());
        assertNotNull(resultado.getReferenciaExterna());
    }

    @Test
    void debeRechazarPagoConImporteMayorAlLimite() {
        ResultadoPasarela resultado = pasarela.procesarPago(
                "clave-003",
                new BigDecimal("150000")
        );

        assertFalse(resultado.isAprobado());
        assertNotNull(resultado.getReferenciaExterna());
    }

    @Test void debeConservarReferenciaAlRepetirPagoYReintegro() {
        var pago = pasarela.procesarPago("estable", new BigDecimal("100"));
        assertEquals(pago.getReferenciaExterna(), pasarela.procesarPago("estable", new BigDecimal("100.00")).getReferenciaExterna());
        var reintegro = pasarela.reintegrarPago("estable", new BigDecimal("100"));
        assertEquals(reintegro.getReferenciaExterna(), pasarela.reintegrarPago("estable", new BigDecimal("100")).getReferenciaExterna());
        assertEquals(reintegro.getReferenciaExterna(), pasarela.consultarReintegro("estable").getReferenciaExterna());
    }

    @Test void debeRechazarClaveConOtroImporteYCompensacionExcesiva() {
        pasarela.procesarPago("importe", new BigDecimal("100"));
        assertThrows(IllegalStateException.class, () -> pasarela.procesarPago("importe", new BigDecimal("200")));
        assertThrows(IllegalArgumentException.class, () -> pasarela.reintegrarPago("importe", new BigDecimal("101")));
        assertThrows(IllegalStateException.class, () -> pasarela.reintegrarPago("inexistente", new BigDecimal("1")));
    }
}
