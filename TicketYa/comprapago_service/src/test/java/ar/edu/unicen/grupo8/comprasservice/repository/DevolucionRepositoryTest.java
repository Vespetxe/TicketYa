
package ar.edu.unicen.grupo8.comprasservice.repository;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion;

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
class DevolucionRepositoryTest {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private DevolucionRepository devolucionRepository;

    @Autowired
    private DetalleDevolucionRepository detalleDevolucionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardarYRecuperarDevolucionConDetalle() {
        Compra compra = new Compra();
        compra.setClienteId(1L);
        compra.setEventoId(10L);
        compra.setReservaId(100L);
        compra.setImporteTotal(new BigDecimal("15000.00"));

        DetalleCompra detalleCompra = new DetalleCompra();
        detalleCompra.setSectorId(5L);
        detalleCompra.setCantidad(2);
        detalleCompra.setPrecioUnitario(new BigDecimal("7500.00"));
        detalleCompra.setSubtotal(new BigDecimal("15000.00"));

        compra.agregarDetalle(detalleCompra);
        compra = compraRepository.saveAndFlush(compra);

        Devolucion devolucion = new Devolucion();
        devolucion.setCompra(compra);
        devolucion.setMotivo("Solicitud del cliente");
        devolucion.setImporteSolicitado(new BigDecimal("7500.00"));

        devolucion = devolucionRepository.saveAndFlush(devolucion);

        DetalleDevolucion detalle = new DetalleDevolucion();
        detalle.setDevolucion(devolucion);
        detalle.setDetalleCompra(detalleCompra);
        detalle.setEntradaId(500L);
        detalle.setImporte(new BigDecimal("7500.00"));

        detalle = detalleDevolucionRepository.saveAndFlush(detalle);

        Long devolucionId = devolucion.getId();
        Long detalleId = detalle.getId();

        entityManager.clear();

        Devolucion recuperada = devolucionRepository
                .findById(devolucionId).orElseThrow();

        DetalleDevolucion detalleRecuperado = detalleDevolucionRepository
                .findById(detalleId).orElseThrow();

        assertEquals(EstadoDevolucion.PENDIENTE, recuperada.getEstado());
        assertEquals("Solicitud del cliente", recuperada.getMotivo());
        assertEquals(compra.getId(), recuperada.getCompra().getId());

        assertEquals(devolucionId,
                detalleRecuperado.getDevolucion().getId());
        assertEquals(detalleCompra.getId(),
                detalleRecuperado.getDetalleCompra().getId());
        assertEquals(500L, detalleRecuperado.getEntradaId());
        assertEquals(0, new BigDecimal("7500.00")
                .compareTo(detalleRecuperado.getImporte()));
    }


    @Test
    void buscarDevolucionesPorCompraYEstado() {
        Compra compra = new Compra();
        compra.setClienteId(10L);
        compra.setEventoId(100L);
        compra.setReservaId(1000L);
        compra.setImporteTotal(new BigDecimal("15000.00"));

        compra = compraRepository.saveAndFlush(compra);

        Devolucion devolucion1 = new Devolucion();
        devolucion1.setCompra(compra);
        devolucion1.setMotivo("Solicitud del cliente");
        devolucion1.setImporteSolicitado(new BigDecimal("7500.00"));

        Devolucion devolucion2 = new Devolucion();
        devolucion2.setCompra(compra);
        devolucion2.setMotivo("Otra solicitud");
        devolucion2.setImporteSolicitado(new BigDecimal("3000.00"));
        devolucion2.setEstado(EstadoDevolucion.RECHAZADA);

        devolucionRepository.saveAndFlush(devolucion1);
        devolucionRepository.saveAndFlush(devolucion2);

        entityManager.clear();

        var devolucionesCompra =
                devolucionRepository.findByCompraId(compra.getId());

        var devolucionesPendientes =
                devolucionRepository.findByEstado(EstadoDevolucion.PENDIENTE);

        var devolucionesEvento =
                devolucionRepository.findByCompraEventoId(100L);

        assertEquals(2, devolucionesCompra.size());

        assertTrue(devolucionesPendientes.stream()
                .anyMatch(d -> d.getId().equals(devolucion1.getId())));

        assertTrue(devolucionesEvento.stream()
                .anyMatch(d -> d.getId().equals(devolucion2.getId())));
    }


    @Test
    void buscarDetallesDeDevolucion() {
        Compra compra = new Compra();
        compra.setClienteId(10L);
        compra.setEventoId(100L);
        compra.setReservaId(1000L);
        compra.setImporteTotal(new BigDecimal("15000.00"));

        DetalleCompra detalleCompra = new DetalleCompra();
        detalleCompra.setSectorId(5L);
        detalleCompra.setCantidad(1);
        detalleCompra.setPrecioUnitario(new BigDecimal("15000.00"));
        detalleCompra.setSubtotal(new BigDecimal("15000.00"));

        compra.agregarDetalle(detalleCompra);
        compra = compraRepository.saveAndFlush(compra);

        Devolucion devolucion = new Devolucion();
        devolucion.setCompra(compra);
        devolucion.setMotivo("Solicitud del cliente");
        devolucion.setImporteSolicitado(new BigDecimal("15000.00"));
        devolucion = devolucionRepository.saveAndFlush(devolucion);

        DetalleDevolucion detalle = new DetalleDevolucion();
        detalle.setDevolucion(devolucion);
        detalle.setDetalleCompra(detalleCompra);
        detalle.setEntradaId(500L);
        detalle.setImporte(new BigDecimal("15000.00"));
        detalle = detalleDevolucionRepository.saveAndFlush(detalle);

        Long detalleId = detalle.getId();

        entityManager.clear();

        var porDevolucion = detalleDevolucionRepository
                .findByDevolucionId(devolucion.getId());

        var porDetalleCompra = detalleDevolucionRepository
                .findByDetalleCompraId(detalleCompra.getId());

        assertTrue(porDevolucion.stream()
                .anyMatch(d -> d.getId().equals(detalleId)));

        assertTrue(porDetalleCompra.stream()
                .anyMatch(d -> d.getId().equals(detalleId)));

        assertTrue(detalleDevolucionRepository.existsByEntradaId(500L));

        assertFalse(detalleDevolucionRepository.existsByEntradaId(999L));
    }


}
