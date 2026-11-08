package example.microservicio_inventario_reservas.services;

import example.microservicio_inventario_reservas.dtos.CrearReservaRequest;
import example.microservicio_inventario_reservas.dtos.ReservaResponse;
import example.microservicio_inventario_reservas.entities.InventarioSector;
import example.microservicio_inventario_reservas.entities.Reserva;
import example.microservicio_inventario_reservas.enums.EstadoInventario;
import example.microservicio_inventario_reservas.enums.EstadoReserva;
import example.microservicio_inventario_reservas.repositories.InventarioSectorRepository;
import example.microservicio_inventario_reservas.repositories.ReservaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private InventarioSectorRepository inventarioSectorRepository;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    @DisplayName("Debe crear una reserva temporal exitosamente en sector no numerado")
    void crearReservaTemporal_SectorNoNumerado_Exito() {
        // ARRANGE (Preparar datos y mocks)
        CrearReservaRequest request = new CrearReservaRequest(10L, 1L, 2L, 3, null);

        InventarioSector inventario = new InventarioSector();
        inventario.setIdInventarioSector(1L);
        inventario.setIdEvento(1L);
        inventario.setIdSector(2L);
        inventario.setEsNumerado(false);
        inventario.setCapacidadTotal(100);
        inventario.setDisponibles(50);
        inventario.setEstado(EstadoInventario.HABILITADO);

        when(inventarioSectorRepository.findByIdEventoAndIdSectorWithLock(1L, 2L))
                .thenReturn(Optional.of(inventario));

        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva r = invocation.getArgument(0);
            r.setIdReserva(99L);
            return r;
        });

        // ACT (Ejecutar el método a probar)
        ReservaResponse response = reservaService.crearReservaTemporal(request);

        // ASSERT (Verificar resultados)
        assertNotNull(response);
        assertEquals(99L, response.getIdReserva());
        assertEquals(EstadoReserva.PENDIENTE, response.getEstado());
        assertEquals(3, response.getCantidad());
        assertEquals(47, inventario.getDisponibles()); // 50 - 3

        verify(inventarioSectorRepository, times(1)).findByIdEventoAndIdSectorWithLock(1L, 2L);
        verify(reservaRepository, times(1)).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si la capacidad disponible es insuficiente")
    void crearReservaTemporal_CapacidadInsuficiente_LanzaExcepcion() {
        // ARRANGE
        CrearReservaRequest request = new CrearReservaRequest(10L, 1L, 2L, 10, null);

        InventarioSector inventario = new InventarioSector();
        inventario.setIdEvento(1L);
        inventario.setIdSector(2L);
        inventario.setDisponibles(5); // Solo quedan 5 y se piden 10
        inventario.setEstado(EstadoInventario.HABILITADO);

        when(inventarioSectorRepository.findByIdEventoAndIdSectorWithLock(1L, 2L))
                .thenReturn(Optional.of(inventario));

        // ACT & ASSERT
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> reservaService.crearReservaTemporal(request)
        );

        assertEquals("Capacidad o aforo insuficiente", exception.getMessage());
        verify(reservaRepository, never()).save(any());
    }
}