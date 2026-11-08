package service.test;

import grupo22.eventocatalogo_service.domain.Event;
import grupo22.eventocatalogo_service.domain.EventSectorPrice;
import grupo22.eventocatalogo_service.domain.EventStatus;
import grupo22.eventocatalogo_service.DTO.request.EventSearchFilterDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSortOption;
import grupo22.eventocatalogo_service.DTO.response.EventSearchResultDTO;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import grupo22.eventocatalogo_service.repository.EventRepository;
import grupo22.eventocatalogo_service.service.EventSearchServiceImpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSearchServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventSearchServiceImpl eventSearchService;

    private Event event1;
    private Event event2;

    @BeforeEach
    void setUp() {
        // Evento 1: Recital (Fecha cercana, Precio mínimo $15.000)
        event1 = new Event("Soda Stereo", "Tour Gracias Totales", "Recital", 100L, 1L, OffsetDateTime.now().plusDays(5));
        event1.setEventId(1L);
        event1.setStatus(EventStatus.PUBLICADO);
        event1.addSectorPrice(new EventSectorPrice(event1, 10L, new BigDecimal("15000.00")));
        event1.addSectorPrice(new EventSectorPrice(event1, 11L, new BigDecimal("30000.00")));

        // Evento 2: Teatro (Fecha lejana, Precio mínimo $8.000)
        event2 = new Event("Toc Toc", "Obra de Teatro", "Teatro", 200L, 2L, OffsetDateTime.now().plusDays(20));
        event2.setEventId(2L);
        event2.setStatus(EventStatus.PUBLICADO);
        event2.addSectorPrice(new EventSectorPrice(event2, 20L, new BigDecimal("8000.00")));
    }

    @Nested
    @DisplayName("Pruebas de Combinación de Filtros")
    class FilterCombinationTests {

        @Test
        @DisplayName("Debe ejecutar la consulta de búsqueda sin filtros enviando Specification genérica")
        void searchEvents_NoFilters_Success() {
            // Given
            EventSearchFilterDTO filter = new EventSearchFilterDTO();
            when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event1, event2));

            // When
            List<EventSearchResultDTO> results = eventSearchService.searchEvents(filter, EventSortOption.DATE_ASC);

            // Then
            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals(1L, results.get(0).getEventId());
            assertEquals(new BigDecimal("15000.00"), results.get(0).getMinPrice());
            assertEquals(new BigDecimal("8000.00"), results.get(1).getMinPrice());

            verify(eventRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("Debe filtrar correctamente por tipo de evento y organizador")
        void searchEvents_FilterByTypeAndOrganizer_Success() {
            // Given
            EventSearchFilterDTO filter = new EventSearchFilterDTO("Recital", 100L, null, null, null, null);
            when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event1));

            // When
            List<EventSearchResultDTO> results = eventSearchService.searchEvents(filter, EventSortOption.DATE_ASC);

            // Then
            assertEquals(1, results.size());
            assertEquals("Soda Stereo", results.get(0).getName());
            assertEquals("Recital", results.get(0).getEventType());
        }

        @Test
        @DisplayName("Debe filtrar por participante y ciudad")
        void searchEvents_FilterByParticipantAndCity_Success() {
            // Given
            EventSearchFilterDTO filter = new EventSearchFilterDTO(null, null, 50L, "Tandil", null, null);
            when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event2));

            // When
            List<EventSearchResultDTO> results = eventSearchService.searchEvents(filter, EventSortOption.DATE_ASC);

            // Then
            assertEquals(1, results.size());
            assertEquals(2L, results.get(0).getEventId());
            assertEquals("Toc Toc", results.get(0).getName());
        }
    }

    @Nested
    @DisplayName("Pruebas de Criterios de Ordenamiento y Cálculo de Precio Mínimo")
    class SortingAndMinPriceTests {

        @Test
        @DisplayName("Debe mapear y calcular correctamente el precio mínimo por sector de un evento")
        void searchEvents_CalculatesMinPriceCorrectly() {
            // Given
            EventSearchFilterDTO filter = new EventSearchFilterDTO();
            when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event1));

            // When
            List<EventSearchResultDTO> results = eventSearchService.searchEvents(filter, EventSortOption.PRICE_ASC);

            // Then
            assertEquals(1, results.size());
            // Evento 1 tiene sectores de 15000 y 30000; el mínimo debe ser 15000
            assertEquals(new BigDecimal("15000.00"), results.get(0).getMinPrice());
        }

        @Test
        @DisplayName("Debe pasar la Specification correspondiente al repositorio según la opción de ordenamiento seleccionada")
        void searchEvents_PassesCorrectSortSpecification() {
            // Given
            EventSearchFilterDTO filter = new EventSearchFilterDTO();
            when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(event2, event1));

            // When
            List<EventSearchResultDTO> results = eventSearchService.searchEvents(filter, EventSortOption.PRICE_DESC);

            // Then
            assertEquals(2, results.size());
            verify(eventRepository).findAll(any(Specification.class));
        }
    }

    @Nested
    @DisplayName("Pruebas de Casos Negativos y Validaciones")
    class NegativeCasesTests {

        @Test
        @DisplayName("Debe lanzar BusinessRuleException (HTTP 400) si fromDate es posterior a toDate")
        void searchEvents_InvalidDateRange_ThrowsException() {
            // Given: fromDate es el 10 de Mayo y toDate es el 1 de Mayo
            OffsetDateTime fromDate = OffsetDateTime.now().plusDays(10);
            OffsetDateTime toDate = OffsetDateTime.now().plusDays(1);
            EventSearchFilterDTO filter = new EventSearchFilterDTO(null, null, null, null, fromDate, toDate);

            // When & Then
            BusinessRuleException exception = assertThrows(BusinessRuleException.class, () ->
                    eventSearchService.searchEvents(filter, EventSortOption.DATE_ASC)
            );

            assertTrue(exception.getMessage().contains("La fecha desde ('fromDate') no puede ser posterior a la fecha hasta ('toDate')"));
            verify(eventRepository, never()).findAll(any(Specification.class));
        }
    }
}