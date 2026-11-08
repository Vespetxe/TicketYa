
package ar.edu.unicen.grupo8.comprasservice.repository;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoPago;

import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import ar.edu.unicen.grupo8.comprasservice.entity.Pago;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PagoRepositoryTest {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardarYRecuperarPago() {
        Compra compra = crearCompra();

        Pago pago = crearPago(compra, "PAGO-TEST-001");
        pago.setMedioPago(ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago.BILLETERA_VIRTUAL);

        Pago guardado = pagoRepository.saveAndFlush(pago);
        Long pagoId = guardado.getId();

        entityManager.clear();

        Pago recuperado = pagoRepository.findById(pagoId).orElseThrow();

        assertNotNull(recuperado.getId());
        assertEquals("PAGO-TEST-001", recuperado.getClaveIdempotencia());
        assertEquals(ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago.BILLETERA_VIRTUAL, recuperado.getMedioPago());
        assertEquals(EstadoPago.PENDIENTE, recuperado.getEstado());
        assertEquals(0, new BigDecimal("15000.00")
                .compareTo(recuperado.getImporte()));
        assertEquals(compra.getId(), recuperado.getCompra().getId());
    }

    @Test
    void rechazarClaveIdempotenciaDuplicada() {
        Compra compra = crearCompra();

        Pago primerPago = crearPago(compra, "CLAVE-DUPLICADA");
        pagoRepository.saveAndFlush(primerPago);

        Pago segundoPago = crearPago(compra, "CLAVE-DUPLICADA");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> pagoRepository.saveAndFlush(segundoPago)
        );
    }

    private Compra crearCompra() {
        Compra compra = new Compra();
        compra.setClienteId(1L);
        compra.setEventoId(10L);
        compra.setReservaId(100L);
        compra.setImporteTotal(new BigDecimal("15000.00"));

        return compraRepository.saveAndFlush(compra);
    }

    private Pago crearPago(Compra compra, String clave) {
        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setClaveIdempotencia(clave);
        pago.setImporte(new BigDecimal("15000.00"));
        return pago;
    }


    @Test
    void buscarPagoPorClaveIdempotencia() {
        Compra compra = new Compra();
        compra.setClienteId(10L);
        compra.setEventoId(100L);
        compra.setReservaId(1000L);
        compra.setImporteTotal(new BigDecimal("15000.00"));

        compra = compraRepository.saveAndFlush(compra);

        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setClaveIdempotencia("PAGO-BUSQUEDA-001");
        pago.setImporte(new BigDecimal("15000.00"));

        pagoRepository.saveAndFlush(pago);

        entityManager.clear();

        var resultado = pagoRepository
                .findByClaveIdempotencia("PAGO-BUSQUEDA-001");

        assertTrue(resultado.isPresent());
        assertEquals("PAGO-BUSQUEDA-001",
                resultado.get().getClaveIdempotencia());

        assertTrue(pagoRepository
                .existsByClaveIdempotencia("PAGO-BUSQUEDA-001"));

        assertFalse(pagoRepository
                .existsByClaveIdempotencia("CLAVE-INEXISTENTE"));
    }

    @Test void conservaRegistroHistoricoSinMedioAlVolverAPersistirlo() {
        var compra = crearCompra(); var pago = pagoRepository.saveAndFlush(crearPago(compra, "historico"));
        Long id = pago.getId(); entityManager.clear();
        var historico = pagoRepository.findById(id).orElseThrow(); assertNull(historico.getMedioPago());
        historico.setReferenciaExterna("referencia-existente"); pagoRepository.saveAndFlush(historico); entityManager.clear();
        var recuperado = pagoRepository.findById(id).orElseThrow();
        assertNull(recuperado.getMedioPago()); assertEquals(id, recuperado.getId()); assertEquals("referencia-existente", recuperado.getReferenciaExterna());
        assertEquals(0, new BigDecimal("15000.00").compareTo(recuperado.getImporte()));
    }
}
