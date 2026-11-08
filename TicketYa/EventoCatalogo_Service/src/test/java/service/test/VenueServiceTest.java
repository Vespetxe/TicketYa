package service.test;

import grupo22.eventocatalogo_service.domain.Seat;
import grupo22.eventocatalogo_service.domain.Sector;
import grupo22.eventocatalogo_service.domain.SectorType;
import grupo22.eventocatalogo_service.domain.Venue;
import grupo22.eventocatalogo_service.DTO.request.SectorRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.SectorResponseDTO;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
import grupo22.eventocatalogo_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import grupo22.eventocatalogo_service.repository.SectorRepository;
import grupo22.eventocatalogo_service.repository.VenueRepository;
import grupo22.eventocatalogo_service.service.VenueService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private SectorRepository sectorRepository;

    @InjectMocks
    private VenueService venueService;

    private Venue mockVenue;

    @BeforeEach
    void setUp() {
        mockVenue = new Venue("Estadio Monumental", "Av. Figueroa Alcorta 7597", new BigDecimal("-34.5453"), new BigDecimal("-58.4497"));
        mockVenue.setVenueId(1L);
    }

    @Nested
    @DisplayName("Pruebas de Sectores Numerados y Auto-Generación de Asientos")
    class NumeratedSectorTests {

        @Test
        @DisplayName("Debe crear sector numerado y auto-generar la grilla correcta de asientos")
        void createNumeratedSector_Success() {
            // Given: Sector numerado de 2 filas x 3 asientos (Capacidad total = 6)
            SectorRequestDTO request = new SectorRequestDTO();
            request.setName("Platea Preferencial");
            request.setType(SectorType.NUMERADO);
            request.setCapacity(6);
            request.setRows(2);
            request.setSeatsPerRow(3);

            when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
            when(sectorRepository.existsByVenueVenueIdAndName(1L, "Platea Preferencial")).thenReturn(false);
            when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> {
                Sector sector = invocation.getArgument(0);
                sector.setSectorId(10L);
                return sector;
            });

            // When
            SectorResponseDTO response = venueService.createSector(1L, request);

            // Then
            assertNotNull(response);
            assertEquals(10L, response.getSectorId());
            assertEquals(SectorType.NUMERADO, response.getType());
            assertEquals(6, response.getCapacity());
            assertEquals(6, response.getTotalSeatsGenerated());

            // Verificamos que se hayan generado exactamente 6 entidades Seat asociadas
            ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
            verify(sectorRepository).save(sectorCaptor.capture());

            Sector savedSector = sectorCaptor.getValue();
            assertEquals(6, savedSector.getSeats().size());

            // Verificar primer asiento (Fila 1, Asiento 1) y último (Fila 2, Asiento 3)
            Seat firstSeat = savedSector.getSeats().get(0);
            assertEquals("Fila 1", firstSeat.getRowLabel());
            assertEquals(1, firstSeat.getSeatNumber());

            Seat lastSeat = savedSector.getSeats().get(5);
            assertEquals("Fila 2", lastSeat.getRowLabel());
            assertEquals(3, lastSeat.getSeatNumber());
        }

        @Test
        @DisplayName("Debe fallar al crear sector numerado sin especificar filas o asientos por fila")
        void createNumeratedSector_MissingGridDetails_ThrowsException() {
            // Given: Sector numerado sin rows ni seatsPerRow
            SectorRequestDTO request = new SectorRequestDTO();
            request.setName("Platea Alta");
            request.setType(SectorType.NUMERADO);
            request.setCapacity(100);

            when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
            when(sectorRepository.existsByVenueVenueIdAndName(1L, "Platea Alta")).thenReturn(false);

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    venueService.createSector(1L, request)
            );

            assertTrue(exception.getMessage().contains("se debe especificar 'rows' y 'seatsPerRow'"));
            verify(sectorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe fallar si la capacidad declarada no coincide con el producto de filas x asientos")
        void createNumeratedSector_MismatchedCapacity_ThrowsException() {
            // Given: Capacidad declarada 50, pero grilla de 10 x 10 = 100
            SectorRequestDTO request = new SectorRequestDTO();
            request.setName("Platea Baja");
            request.setType(SectorType.NUMERADO);
            request.setCapacity(50);
            request.setRows(10);
            request.setSeatsPerRow(10);

            when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
            when(sectorRepository.existsByVenueVenueIdAndName(1L, "Platea Baja")).thenReturn(false);

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    venueService.createSector(1L, request)
            );

            assertTrue(exception.getMessage().contains("no coincide con el producto de filas"));
            verify(sectorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Pruebas de Sectores No Numerados")
    class NonNumeratedSectorTests {

        @Test
        @DisplayName("Debe crear sector no numerado exitosamente sin generar asientos")
        void createNonNumeratedSector_Success() {
            // Given
            SectorRequestDTO request = new SectorRequestDTO();
            request.setName("Campo General");
            request.setType(SectorType.NO_NUMERADO);
            request.setCapacity(5000);

            when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
            when(sectorRepository.existsByVenueVenueIdAndName(1L, "Campo General")).thenReturn(false);
            when(sectorRepository.save(any(Sector.class))).thenAnswer(invocation -> {
                Sector sector = invocation.getArgument(0);
                sector.setSectorId(20L);
                return sector;
            });

            // When
            SectorResponseDTO response = venueService.createSector(1L, request);

            // Then
            assertNotNull(response);
            assertEquals(20L, response.getSectorId());
            assertEquals(SectorType.NO_NUMERADO, response.getType());
            assertEquals(5000, response.getCapacity());
            assertEquals(0, response.getTotalSeatsGenerated());

            ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
            verify(sectorRepository).save(sectorCaptor.capture());
            assertTrue(sectorCaptor.getValue().getSeats().isEmpty());
        }

        @Test
        @DisplayName("Debe fallar si se envían datos de filas o asientos en un sector no numerado")
        void createNonNumeratedSector_WithGridDetails_ThrowsException() {
            // Given: Sector no numerado al que le pasaron filas por error
            SectorRequestDTO request = new SectorRequestDTO();
            request.setName("Popular Norte");
            request.setType(SectorType.NO_NUMERADO);
            request.setCapacity(3000);
            request.setRows(10);

            when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
            when(sectorRepository.existsByVenueVenueIdAndName(1L, "Popular Norte")).thenReturn(false);

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    venueService.createSector(1L, request)
            );

            assertTrue(exception.getMessage().contains("no debe definir filas ni asientos"));
            verify(sectorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Pruebas de Reglas de Negocio Generales")
    class GeneralBusinessRulesTests {

        @Test
        @DisplayName("Debe lanzar ResourceNotFoundException si el lugar no existe")
        void createSector_VenueNotFound_ThrowsException() {
            // Given
            SectorRequestDTO request = new SectorRequestDTO();
            request.setName("Campo");
            request.setType(SectorType.NO_NUMERADO);
            request.setCapacity(100);

            when(venueRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () ->
                    venueService.createSector(99L, request)
            );
            verify(sectorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar BusinessRuleException si ya existe un sector con el mismo nombre en el lugar")
        void createSector_DuplicateName_ThrowsException() {
            // Given
            SectorRequestDTO request = new SectorRequestDTO();
            request.setName("Campo General");
            request.setType(SectorType.NO_NUMERADO);
            request.setCapacity(1000);

            when(venueRepository.findById(1L)).thenReturn(Optional.of(mockVenue));
            when(sectorRepository.existsByVenueVenueIdAndName(1L, "Campo General")).thenReturn(true);

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    venueService.createSector(1L, request)
            );

            assertTrue(exception.getMessage().contains("Ya existe un sector llamado"));
            verify(sectorRepository, never()).save(any());
        }
    }
}