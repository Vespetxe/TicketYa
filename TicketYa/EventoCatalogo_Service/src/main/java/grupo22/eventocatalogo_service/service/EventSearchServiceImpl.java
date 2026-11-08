package grupo22.eventocatalogo_service.service;

import grupo22.eventocatalogo_service.domain.Event;
import grupo22.eventocatalogo_service.domain.EventSectorPrice;

import grupo22.eventocatalogo_service.DTO.request.EventSearchFilterDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSortOption;
import grupo22.eventocatalogo_service.DTO.response.EventSearchResultDTO;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import grupo22.eventocatalogo_service.repository.EventRepository;
import grupo22.eventocatalogo_service.repository.EventSpecificationBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventSearchServiceImpl implements EventSearchService {

    private final EventRepository eventRepository;

    public EventSearchServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSearchResultDTO> searchEvents(EventSearchFilterDTO filter, EventSortOption sortOption) {
        // Validación de Rango de Fechas (fromDate debe ser anterior o igual a toDate)
        if (filter.getFromDate() != null && filter.getToDate() != null && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new BusinessRuleException("La fecha desde ('fromDate') no puede ser posterior a la fecha hasta ('toDate')");
        }

        Specification<Event> spec = EventSpecificationBuilder.buildSpecification(filter, sortOption);
        List<Event> events = eventRepository.findAll(spec);

        return events.stream()
                .map(this::mapToSearchResultDTO)
                .collect(Collectors.toList());
    }

    private EventSearchResultDTO mapToSearchResultDTO(Event event) {
        // Calcular el precio mínimo del evento recorriendo sus sectores configurados
        BigDecimal minPrice = event.getSectorPrices().stream()
                .map(EventSectorPrice::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new EventSearchResultDTO(
                event.getEventId(),
                event.getName(),
                event.getEventType(),
                event.getVenueId(),
                event.getStartDateTime(),
                event.getStatus(),
                minPrice
        );
    }
}
