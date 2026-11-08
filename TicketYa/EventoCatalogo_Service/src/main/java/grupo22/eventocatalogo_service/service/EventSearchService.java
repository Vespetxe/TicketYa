package grupo22.eventocatalogo_service.service;

import grupo22.eventocatalogo_service.DTO.request.EventSearchFilterDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSortOption;
import grupo22.eventocatalogo_service.DTO.response.EventSearchResultDTO;

import java.util.List;

public interface EventSearchService {
    List<EventSearchResultDTO> searchEvents(EventSearchFilterDTO filter, EventSortOption sortOption);
}
