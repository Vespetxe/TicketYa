package example.microservicio_inventario_reservas.services;

import example.microservicio_inventario_reservas.dtos.*;
import example.microservicio_inventario_reservas.entities.Asiento;
import example.microservicio_inventario_reservas.entities.InventarioSector;
import example.microservicio_inventario_reservas.enums.EstadoAsiento;
import example.microservicio_inventario_reservas.repositories.AsientoRepository;
import example.microservicio_inventario_reservas.repositories.InventarioSectorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioSectorRepository inventarioSectorRepository;

    @Mock
    private AsientoRepository asientoRepository;
    @Mock private DevolucionInventarioService devoluciones;
    @Mock private ControlEventoService control;
    @Mock private example.microservicio_inventario_reservas.repositories.ReservaRepository reservas;

    @InjectMocks
    private InventarioService inventarioService;

    @Test
    @DisplayName("Debe inicializar inventario numerado con su grilla de asientos correctamente")
    void inicializarInventario_Numerado_Exito() {
        // ARRANGE
        Long idEvento = 1L;
        AsientoDTO asientoDTO = new AsientoDTO("A", 10);
        InicializarInventarioRequest request = new InicializarInventarioRequest(2L, true, 1, List.of(asientoDTO));

        when(inventarioSectorRepository.findByIdEventoAndIdSector(idEvento, 2L))
                .thenReturn(Optional.empty());

        when(inventarioSectorRepository.save(any(InventarioSector.class))).thenAnswer(i -> i.getArgument(0));

        // ACT
        MensajeResponse response = inventarioService.inicializarInventario(idEvento, request);

        // ASSERT
        assertNotNull(response);
        assertTrue(response.isExitoso());
        assertEquals("Inventario inicializado exitosamente", response.getMensaje());

        verify(inventarioSectorRepository, times(1)).save(any(InventarioSector.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar inicializar un inventario que ya existe")
    void inicializarInventario_YaExiste_LanzaExcepcion() {
        // ARRANGE
        Long idEvento = 1L;
        InicializarInventarioRequest request = new InicializarInventarioRequest(2L, false, 100, null);

        when(inventarioSectorRepository.findByIdEventoAndIdSector(idEvento, 2L))
                .thenReturn(Optional.of(new InventarioSector()));

        // ACT & ASSERT
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventarioService.inicializarInventario(idEvento, request)
        );

        assertEquals("El inventario para este evento y sector ya existe", exception.getMessage());
        verify(inventarioSectorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe consultar disponibilidad de un sector numerado retornando los asientos libres")
    void consultarDisponibilidad_SectorNumerado_Exito() {
        // ARRANGE
        Long idEvento = 1L;
        Long idSector = 2L;

        InventarioSector inventario = new InventarioSector();
        inventario.setIdEvento(idEvento);
        inventario.setIdSector(idSector);
        inventario.setEsNumerado(true);
        inventario.setCapacidadTotal(100);
        inventario.setDisponibles(10);

        Asiento libre = new Asiento();
        libre.setIdAsiento(5L);
        libre.setFila("B");
        libre.setNumero(15);
        libre.setEstado(EstadoAsiento.LIBRE);

        Asiento ocupado = new Asiento();
        ocupado.setIdAsiento(6L);
        ocupado.setFila("B");
        ocupado.setNumero(16);
        ocupado.setEstado(EstadoAsiento.OCUPADO);

        inventario.setAsientos(List.of(libre, ocupado));

        when(inventarioSectorRepository.findByIdEventoAndIdSector(idEvento, idSector))
                .thenReturn(Optional.of(inventario));

        // ACT
        DisponibilidadResponse response = inventarioService.consultarDisponibilidad(idEvento, idSector);

        // ASSERT
        assertNotNull(response);
        assertEquals(10, response.getDisponibles());
        assertTrue(response.isEsNumerado());
        assertEquals(1, response.getAsientosLibres().size());
        assertEquals(5L, response.getAsientosLibres().get(0).getIdAsiento());
    }

    @Test
    @DisplayName("Debe incrementar disponibilidad al procesar una devolución")
    void liberarPorDevolucion_Exito() {
        // ARRANGE
        LiberarDevolucionRequest request = new LiberarDevolucionRequest(1L, 2L, 2, List.of(10L, 11L));
        request.setIdReserva(9L); request.setEntradasIds(List.of("ticket-1","ticket-2"));
        var reserva=new example.microservicio_inventario_reservas.entities.Reserva();
        reserva.setIdEvento(1L); reserva.setIdSector(2L);
        when(reservas.buscarConBloqueo(9L)).thenReturn(Optional.of(reserva));

        // ACT
        MensajeResponse response = inventarioService.liberarPorDevolucion(request);

        // ASSERT
        assertTrue(response.isExitoso());
        verify(devoluciones).devolver(9L,new DevolverLugaresRequest(List.of(
                new DevolverLugaresRequest.Unidad("ticket-1",10L),new DevolverLugaresRequest.Unidad("ticket-2",11L))));
    }
}
