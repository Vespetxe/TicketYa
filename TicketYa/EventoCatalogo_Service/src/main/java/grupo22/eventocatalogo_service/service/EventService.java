package grupo22.eventocatalogo_service.service;

import grupo22.eventocatalogo_service.DTO.request.EventRequestDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSectorPriceRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.EventResponseDTO;
import grupo22.eventocatalogo_service.DTO.response.EventSectorPriceResponseDTO;

import java.util.List;

public interface EventService {
    EventResponseDTO createEvent(EventRequestDTO request);
    EventResponseDTO getEventById(Long eventId);
    List<EventSectorPriceResponseDTO> setSectorPrices(Long eventId, List<EventSectorPriceRequestDTO> prices);
    EventResponseDTO publishEvent(Long eventId);
    EventResponseDTO cancelEvent(Long eventId, String userRole);
    EventResponseDTO suspendOrUpdateStatus(Long eventId, String newStatus, String userRole);
}