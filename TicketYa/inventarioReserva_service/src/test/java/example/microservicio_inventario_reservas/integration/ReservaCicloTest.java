package example.microservicio_inventario_reservas.integration;

import example.microservicio_inventario_reservas.dtos.CrearReservaRequest;
import example.microservicio_inventario_reservas.entities.*;
import example.microservicio_inventario_reservas.enums.*;
import example.microservicio_inventario_reservas.repositories.*;
import example.microservicio_inventario_reservas.services.ReservaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReservaCicloTest {
    @Autowired ReservaService service;
    @Autowired InventarioSectorRepository inventarios;
    @Autowired ReservaRepository reservas;
    @Autowired PlatformTransactionManager transactions;

    private InventarioSector inventario(boolean numerado) {
        InventarioSector i = new InventarioSector();
        long id = Math.abs(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        i.setIdEvento(id); i.setIdSector(id); i.setCapacidadTotal(1); i.setDisponibles(1);
        i.setEsNumerado(numerado); i.setEstado(EstadoInventario.HABILITADO);
        if (numerado) {
            Asiento a = new Asiento(); a.setFila("A"); a.setNumero(1); a.setEstado(EstadoAsiento.LIBRE);
            i.addAsiento(a);
        }
        return inventarios.saveAndFlush(i);
    }
    private Long reservar(InventarioSector i) {
        return service.crearReservaTemporal(new CrearReservaRequest(1L, i.getIdEvento(), i.getIdSector(), 1,
                i.isEsNumerado() ? List.of(i.getAsientos().get(0).getIdAsiento()) : null)).getIdReserva();
    }
    private void vencer(Long id) {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            Reserva r = reservas.buscarConBloqueo(id).orElseThrow();
            r.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        });
    }
    @Test void rechazaCantidadesInvalidasSinCambiarStock() {
        InventarioSector i = inventario(false);
        for (int cantidad : new int[]{0, -1})
            assertThrows(IllegalArgumentException.class, () -> service.crearReservaTemporal(
                    new CrearReservaRequest(1L, i.getIdEvento(), i.getIdSector(), cantidad, null)));
        assertEquals(1, inventarios.findById(i.getIdInventarioSector()).orElseThrow().getDisponibles());
    }
    @Test void liberarNumeradoReponeStockUnaSolaVezYPermiteReservarOtraVez() {
        InventarioSector i = inventario(true); Long id = reservar(i);
        service.liberarReserva(id); service.liberarReserva(id);
        assertEquals(1, inventarios.findById(i.getIdInventarioSector()).orElseThrow().getDisponibles());
        assertNotNull(reservar(i));
    }
    @Test void confirmarEsIdempotenteYNoPermiteLiberacionTemporal() {
        Long id = reservar(inventario(false));
        service.confirmarReserva(id); service.confirmarReserva(id);
        assertThrows(IllegalStateException.class, () -> service.liberarReserva(id));
    }
    @Test void vencimientoImpideConfirmarYReponeStockSinDuplicarlo() {
        InventarioSector i = inventario(true); Long id = reservar(i); vencer(id);
        assertThrows(IllegalStateException.class, () -> service.confirmarReserva(id));
        service.expirarReservasVencidas(); service.expirarReservasVencidas();
        assertEquals(EstadoReserva.EXPIRADA, reservas.findById(id).orElseThrow().getEstado());
        assertEquals(1, inventarios.findById(i.getIdInventarioSector()).orElseThrow().getDisponibles());
    }
    @Test void inventarioCanceladoImpideConfirmacionYNoReabreStock() {
        InventarioSector i = inventario(false); Long id = reservar(i);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            inventarios.cancelarInventarioPorEvento(i.getIdEvento());
        });
        assertThrows(IllegalStateException.class, () -> service.confirmarReserva(id));
        service.liberarReserva(id);
        assertEquals(0, inventarios.findById(i.getIdInventarioSector()).orElseThrow().getDisponibles());
    }
    @Test void compensarReservaExpiradaNoDuplicaLaReposicion() {
        InventarioSector i=inventario(false);Long id=reservar(i);vencer(id);
        service.expirarReservasVencidas();service.compensarReserva(id);service.compensarReserva(id);
        assertEquals(EstadoReserva.LIBERADA,reservas.findById(id).orElseThrow().getEstado());
        assertEquals(1,inventarios.findById(i.getIdInventarioSector()).orElseThrow().getDisponibles());
    }
    @Test void liberacionesConcurrentesNoDuplicanStock() throws Exception {
        InventarioSector i = inventario(false); Long id = reservar(i);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> trabajos = new java.util.ArrayList<>();
            for (int n = 0; n < 8; n++) trabajos.add(executor.submit(() -> {
                start.await(); service.liberarReserva(id); return null;
            }));
            start.countDown();
            for (Future<?> trabajo : trabajos) trabajo.get(15, TimeUnit.SECONDS);
            assertEquals(1, inventarios.findById(i.getIdInventarioSector()).orElseThrow().getDisponibles());
        } finally { executor.shutdownNow(); }
    }
}
