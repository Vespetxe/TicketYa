
package ar.edu.unicen.grupo8.comprasservice.repository;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoReintegro;

import ar.edu.unicen.grupo8.comprasservice.entity.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReintegroRepositoryTest {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private DevolucionRepository devolucionRepository;

    @Autowired
    private ReintegroRepository reintegroRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void rechazarClaveIdempotenciaDuplicada() {
        Compra compra = new Compra(); compra.setClienteId(1L); compra.setEventoId(10L); compra.setReservaId(998L);
        compra.setImporteTotal(new BigDecimal("100.00")); compraRepository.saveAndFlush(compra);
        Pago pago = new Pago(); pago.setCompra(compra); pago.setImporte(new BigDecimal("100.00"));
        pago.setClaveIdempotencia("pago-unicidad-reintegro"); pagoRepository.saveAndFlush(pago);
        Reintegro primero = new Reintegro(); primero.setPago(pago); primero.setClaveIdempotencia("reintegro-unico");
        primero.setImporte(new BigDecimal("100.00")); reintegroRepository.saveAndFlush(primero);
        Reintegro segundo = new Reintegro(); segundo.setPago(pago); segundo.setClaveIdempotencia("reintegro-unico");
        segundo.setImporte(new BigDecimal("100.00"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> reintegroRepository.saveAndFlush(segundo));
    }

    @Test
    void guardarYRecuperarReintegro() {
        Compra compra = new Compra();
        compra.setClienteId(1L);
        compra.setEventoId(10L);
        compra.setReservaId(100L);
        compra.setImporteTotal(new BigDecimal("15000.00"));
        compra = compraRepository.saveAndFlush(compra);

        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setClaveIdempotencia("PAGO-REINTEGRO-001");
        pago.setImporte(new BigDecimal("15000.00"));
        pago = pagoRepository.saveAndFlush(pago);

        Devolucion devolucion = new Devolucion();
        devolucion.setCompra(compra);
        devolucion.setMotivo("Solicitud del cliente");
        devolucion.setImporteSolicitado(new BigDecimal("7500.00"));
        devolucion = devolucionRepository.saveAndFlush(devolucion);

        Reintegro reintegro = new Reintegro();
        reintegro.setPago(pago);
        reintegro.setDevolucion(devolucion);
        reintegro.setClaveIdempotencia("REINTEGRO-001");
        reintegro.setImporte(new BigDecimal("7500.00"));

        reintegro = reintegroRepository.saveAndFlush(reintegro);
        Long reintegroId = reintegro.getId();

        entityManager.clear();

        Reintegro recuperado = reintegroRepository
                .findById(reintegroId).orElseThrow();

        assertNotNull(recuperado.getId());
        assertEquals(EstadoReintegro.PENDIENTE, recuperado.getEstado());
        assertEquals("REINTEGRO-001", recuperado.getClaveIdempotencia());
        assertEquals(0, new BigDecimal("7500.00")
                .compareTo(recuperado.getImporte()));
        assertEquals(pago.getId(), recuperado.getPago().getId());
        assertEquals(devolucion.getId(), recuperado.getDevolucion().getId());
    }


    @Test
    void buscarReintegrosPorPagoDevolucionYClave() {
        Compra compra = new Compra();
        compra.setClienteId(10L);
        compra.setEventoId(100L);
        compra.setReservaId(1000L);
        compra.setImporteTotal(new BigDecimal("15000.00"));
        compra = compraRepository.saveAndFlush(compra);

        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setClaveIdempotencia("PAGO-CONSULTA-001");
        pago.setImporte(new BigDecimal("15000.00"));
        pago = pagoRepository.saveAndFlush(pago);

        Devolucion devolucion = new Devolucion();
        devolucion.setCompra(compra);
        devolucion.setMotivo("Solicitud del cliente");
        devolucion.setImporteSolicitado(new BigDecimal("7500.00"));
        devolucion = devolucionRepository.saveAndFlush(devolucion);

        Reintegro reintegro = new Reintegro();
        reintegro.setPago(pago);
        reintegro.setDevolucion(devolucion);
        reintegro.setClaveIdempotencia("REINTEGRO-CONSULTA-001");
        reintegro.setImporte(new BigDecimal("7500.00"));
        reintegro = reintegroRepository.saveAndFlush(reintegro);

        Long reintegroId = reintegro.getId();

        entityManager.clear();

        var porPago = reintegroRepository.findByPagoId(pago.getId());
        var porDevolucion = reintegroRepository
                .findByDevolucionId(devolucion.getId());
        var porClave = reintegroRepository
                .findByClaveIdempotencia("REINTEGRO-CONSULTA-001");

        assertTrue(porPago.stream()
                .anyMatch(r -> r.getId().equals(reintegroId)));

        assertTrue(porDevolucion.stream()
                .anyMatch(r -> r.getId().equals(reintegroId)));

        assertTrue(porClave.isPresent());
        assertEquals(reintegroId, porClave.get().getId());

        assertTrue(reintegroRepository
                .existsByClaveIdempotencia("REINTEGRO-CONSULTA-001"));

        assertFalse(reintegroRepository
                .existsByClaveIdempotencia("CLAVE-INEXISTENTE"));
    }

}
