
package ar.edu.unicen.grupo8.comprasservice.repository;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoCompra;

import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import ar.edu.unicen.grupo8.comprasservice.entity.DetalleCompra;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CompraRepositoryTest {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardarYRecuperarCompraConDetalles() {

        // 1. Crear una compra
        Compra compra = new Compra();
        compra.setClienteId(1L);
        compra.setEventoId(10L);
        compra.setReservaId(100L);
        compra.setImporteTotal(new BigDecimal("15000.00"));

        // 2. Crear un detalle
        DetalleCompra detalle = new DetalleCompra();
        detalle.setSectorId(5L);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("7500.00"));
        detalle.setSubtotal(new BigDecimal("15000.00"));

        // 3. Asociar el detalle con la compra
        compra.agregarDetalle(detalle);

        // 4. Guardar la compra y sus detalles
        Compra guardada = compraRepository.saveAndFlush(compra);
        // Limpiar la caché de primer nivel de Hibernate
        entityManager.clear();

        // 5. Recuperar la compra desde el repositorio
        Compra recuperada = compraRepository.findById(
                guardada.getId()
        ).orElseThrow();

        // 6. Verificar los datos
        assertNotNull(recuperada.getId());
        assertEquals(1L, recuperada.getClienteId());
        assertEquals(EstadoCompra.PENDIENTE, recuperada.getEstado());
        assertEquals(
                0,
                new BigDecimal("15000.00")
                        .compareTo(recuperada.getImporteTotal())
        );

        // 7. Verificar la relación con DetalleCompra
        assertEquals(1, recuperada.getDetalles().size());

        DetalleCompra detalleRecuperado =
                recuperada.getDetalles().get(0);

        assertNotNull(detalleRecuperado.getId());
        assertEquals(5L, detalleRecuperado.getSectorId());
        assertEquals(2, detalleRecuperado.getCantidad());
        assertEquals(
                recuperada.getId(),
                detalleRecuperado.getCompra().getId()
        );
    }


    @Test
    void buscarComprasPorCliente() {
        Compra compra1 = new Compra();
        compra1.setClienteId(10L);
        compra1.setEventoId(100L);
        compra1.setReservaId(1000L);
        compra1.setImporteTotal(new BigDecimal("15000.00"));

        Compra compra2 = new Compra();
        compra2.setClienteId(10L);
        compra2.setEventoId(101L);
        compra2.setReservaId(1001L);
        compra2.setImporteTotal(new BigDecimal("20000.00"));

        Compra compra3 = new Compra();
        compra3.setClienteId(20L);
        compra3.setEventoId(102L);
        compra3.setReservaId(1002L);
        compra3.setImporteTotal(new BigDecimal("10000.00"));

        compraRepository.saveAndFlush(compra1);
        compraRepository.saveAndFlush(compra2);
        compraRepository.saveAndFlush(compra3);

        entityManager.clear();

        var comprasCliente = compraRepository.findByClienteId(10L);

        assertEquals(2, comprasCliente.size());
        assertTrue(comprasCliente.stream()
                .allMatch(compra -> compra.getClienteId().equals(10L)));
    }

}
