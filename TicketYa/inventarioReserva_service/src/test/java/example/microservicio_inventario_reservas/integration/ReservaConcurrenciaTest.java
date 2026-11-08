package example.microservicio_inventario_reservas.integration;

import example.microservicio_inventario_reservas.dtos.CrearReservaRequest;
import example.microservicio_inventario_reservas.entities.InventarioSector;
import example.microservicio_inventario_reservas.enums.EstadoInventario;
import example.microservicio_inventario_reservas.repositories.InventarioSectorRepository;
import example.microservicio_inventario_reservas.repositories.ReservaRepository;
import example.microservicio_inventario_reservas.services.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ReservaConcurrenciaTest {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private InventarioSectorRepository inventarioSectorRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        inventarioSectorRepository.deleteAll();

        // Inicializar sector con SOLO 1 LUGAR DISPONIBLE
        InventarioSector inventario = new InventarioSector();
        inventario.setIdEvento(100L);
        inventario.setIdSector(200L);
        inventario.setEsNumerado(false);
        inventario.setCapacidadTotal(1);
        inventario.setDisponibles(1);
        inventario.setEstado(EstadoInventario.HABILITADO);
        inventarioSectorRepository.save(inventario);
    }

    @Test
    @DisplayName("Simulación de 10 peticiones concurrentes: Solo 1 debe lograr la reserva y las otras 9 rebotar")
    void testConcurrenciaPrevencionDobleVenta() throws InterruptedException {
        int cantidadHilos = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(cantidadHilos);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger fallos = new AtomicInteger(0);

        for (int i = 0; i < cantidadHilos; i++) {
            final long clienteId = i + 1;
            executorService.submit(() -> {
                try {
                    latch.await(); // Esperar a que todos los hilos estén listos para disparar simultáneamente
                    CrearReservaRequest request = new CrearReservaRequest(clienteId, 100L, 200L, 1, null);
                    reservaService.crearReservaTemporal(request);
                    exitos.incrementAndGet();
                } catch (Exception e) {
                    fallos.incrementAndGet();
                }
            });
        }

        latch.countDown(); // Liberar todos los hilos al mismo tiempo
        executorService.shutdown();
        while (!executorService.isTerminated()) {
            Thread.sleep(50);
        }

        // VERIFICACIONES
        assertEquals(1, exitos.get(), "Solo 1 cliente debió lograr la reserva");
        assertEquals(9, fallos.get(), "Los otros 9 intentos debieron rebotar por falta de cupo");

        InventarioSector inventarioFinal = inventarioSectorRepository.findByIdEventoAndIdSector(100L, 200L).orElseThrow();
        assertEquals(0, inventarioFinal.getDisponibles(), "El stock final debe ser exactamente 0");
    }
}