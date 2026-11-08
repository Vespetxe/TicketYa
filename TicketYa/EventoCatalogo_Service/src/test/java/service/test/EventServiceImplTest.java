package service.test;

import grupo22.eventocatalogo_service.domain.Event;
import grupo22.eventocatalogo_service.domain.EventSectorPrice;
import grupo22.eventocatalogo_service.domain.EventStatus;
import grupo22.eventocatalogo_service.domain.Sector;
import grupo22.eventocatalogo_service.domain.SectorType;
import grupo22.eventocatalogo_service.domain.Venue;
import grupo22.eventocatalogo_service.DTO.response.EventResponseDTO;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
import grupo22.eventocatalogo_service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import grupo22.eventocatalogo_service.repository.EventRepository;
import grupo22.eventocatalogo_service.repository.EventSectorPriceRepository;
import grupo22.eventocatalogo_service.repository.ParticipantRepository;
import grupo22.eventocatalogo_service.repository.SectorRepository;
import grupo22.eventocatalogo_service.repository.VenueRepository;
import grupo22.eventocatalogo_service.service.EventServiceImpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private EventSectorPriceRepository eventSectorPriceRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event mockEvent;
    private Sector mockSector1;
    private Sector mockSector2;

    @BeforeEach
    void setUp() {
        mockEvent = new Event("Recital Rock", "Descripción del evento", "Recital", 100L, 1L, OffsetDateTime.now().plusDays(10));
        mockEvent.setEventId(10L);
        mockEvent.setStatus(EventStatus.BORRADOR);

        Venue mockVenue = new Venue("Estadio", "Dirección", new BigDecimal("-34.0"), new BigDecimal("-58.0"));
        mockVenue.setVenueId(1L);

        mockSector1 = new Sector(mockVenue, "Campo", SectorType.NO_NUMERADO, 1000);
        mockSector1.setSectorId(101L);

        mockSector2 = new Sector(mockVenue, "Platea", SectorType.NUMERADO, 200);
        mockSector2.setSectorId(102L);
    }

    @Nested
    @DisplayName("Pruebas de Publicación de Eventos (BORRADOR -> PUBLICADO)")
    class PublishEventTests {

        @Test
        @DisplayName("Caso Positivo: Debe publicar un evento cuando todos sus sectores tienen precio")
        void publishEvent_Success() {
            // Given: Evento en BORRADOR con precios configurados para los 2 sectores del lugar
            mockEvent.addSectorPrice(new EventSectorPrice(mockEvent, 101L, new BigDecimal("15000.00")));
            mockEvent.addSectorPrice(new EventSectorPrice(mockEvent, 102L, new BigDecimal("30000.00")));

            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
            when(sectorRepository.findByVenueVenueId(1L)).thenReturn(List.of(mockSector1, mockSector2));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            EventResponseDTO response = eventService.publishEvent(10L);

            // Then
            assertNotNull(response);
            assertEquals(EventStatus.PUBLICADO, response.getStatus());
            verify(eventRepository).save(mockEvent);
        }

        @Test
        @DisplayName("Caso Negativo: Debe fallar al publicar si falta configurar precio en algún sector")
        void publishEvent_MissingSectorPrice_ThrowsException() {
            // Given: Solo el sector 101 tiene precio; falta el sector 102
            mockEvent.addSectorPrice(new EventSectorPrice(mockEvent, 101L, new BigDecimal("15000.00")));

            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
            when(sectorRepository.findByVenueVenueId(1L)).thenReturn(List.of(mockSector1, mockSector2));

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    eventService.publishEvent(10L)
            );

            assertTrue(exception.getMessage().contains("Faltan configurar precios para los siguientes sectores"));
            assertTrue(exception.getMessage().contains("Platea"));
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Caso Negativo: Debe fallar al intentar publicar un evento que no está en BORRADOR")
        void publishEvent_InvalidCurrentStatus_ThrowsException() {
            // Given: Evento que ya está PUBLICADO
            mockEvent.setStatus(EventStatus.PUBLICADO);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    eventService.publishEvent(10L)
            );

            assertTrue(exception.getMessage().contains("Transición inválida"));
            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Pruebas de Cancelación de Eventos (CANCELADO)")
    class CancelEventTests {

        @Test
        @DisplayName("Caso Positivo: Un Administrador (ADMIN) puede cancelar un evento PUBLICADO")
        void cancelEvent_AsAdmin_Success() {
            // Given
            mockEvent.setStatus(EventStatus.PUBLICADO);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            EventResponseDTO response = eventService.cancelEvent(10L, "ADMIN");

            // Then
            assertNotNull(response);
            assertEquals(EventStatus.CANCELADO, response.getStatus());
            verify(eventRepository).save(mockEvent);
        }

        @Test
        @DisplayName("Caso Negativo: Debe denegar la cancelación si el rol no es ADMIN")
        void cancelEvent_NonAdminRole_ThrowsException() {
            // Given: Intento de cancelación por parte de un CLIENTE u ORGANIZADOR
            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    eventService.cancelEvent(10L, "ORGANIZER")
            );

            assertTrue(exception.getMessage().contains("Permiso denegado"));
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Caso Negativo: Debe fallar al intentar cancelar un evento ya FINALIZADO")
        void cancelEvent_AlreadyFinished_ThrowsException() {
            // Given: Evento en estado terminal FINALIZADO
            mockEvent.setStatus(EventStatus.FINALIZADO);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    eventService.cancelEvent(10L, "ADMIN")
            );

            assertTrue(exception.getMessage().contains("No se puede cancelar un evento que ya ha FINALIZADO"));
            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Pruebas Generales de Transiciones en Máquina de Estados")
    class StateMachineTransitionsTest {

        @Test
        @DisplayName("Caso Positivo: Transicionar de PUBLICADO a AGOTADO")
        void transition_PublishedToSoldOut_Success() {
            // Given
            mockEvent.setStatus(EventStatus.PUBLICADO);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            EventResponseDTO response = eventService.suspendOrUpdateStatus(10L, "AGOTADO", "ADMIN");

            // Then
            assertEquals(EventStatus.AGOTADO, response.getStatus());
        }

        @Test
        @DisplayName("Caso Negativo: Transición no permitida de FINALIZADO a PUBLICADO")
        void transition_FinishedToPublished_ThrowsException() {
            // Given
            mockEvent.setStatus(EventStatus.FINALIZADO);
            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    eventService.suspendOrUpdateStatus(10L, "PUBLICADO", "ADMIN")
            );

            assertTrue(exception.getMessage().contains("estado terminal"));
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Caso Negativo: Error si se especifica un estado inexistente")
        void transition_InvalidStatusString_ThrowsException() {
            // Given
            when(eventRepository.findById(10L)).thenReturn(Optional.of(mockEvent));

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    eventService.suspendOrUpdateStatus(10L, "ESTADO_INVENTADO", "ADMIN")
            );

            assertTrue(exception.getMessage().contains("Estado no permitido"));
        }

        @Test
        @DisplayName("Caso Negativo: Lanzar ResourceNotFoundException si el evento no existe")
        void eventNotFound_ThrowsException() {
            // Given
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(ResourceNotFoundException.class, () ->
                    eventService.getEventById(99L)
            );
        }
    }
}