package grupo22.eventocatalogo_service.service;

import grupo22.eventocatalogo_service.DTO.request.EventRequestDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSectorPriceRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.EventResponseDTO;
import grupo22.eventocatalogo_service.DTO.response.EventSectorPriceResponseDTO;
import grupo22.eventocatalogo_service.domain.*;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
import grupo22.eventocatalogo_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import grupo22.eventocatalogo_service.repository.EventRepository;
import grupo22.eventocatalogo_service.repository.EventSectorPriceRepository;
import grupo22.eventocatalogo_service.repository.ParticipantRepository;
import grupo22.eventocatalogo_service.repository.SectorRepository;
import grupo22.eventocatalogo_service.repository.VenueRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SectorRepository sectorRepository;
    private final ParticipantRepository participantRepository;
    private final EventSectorPriceRepository eventSectorPriceRepository;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            SectorRepository sectorRepository,
            ParticipantRepository participantRepository,
            EventSectorPriceRepository eventSectorPriceRepository) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.sectorRepository = sectorRepository;
        this.participantRepository = participantRepository;
        this.eventSectorPriceRepository = eventSectorPriceRepository;
    }

    @Override
    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO request) {
        if (!venueRepository.existsById(request.getVenueId())) {
            throw new ResourceNotFoundException("El lugar con ID " + request.getVenueId() + " no existe");
        }

        Event event = new Event(
                request.getName(),
                request.getDescription(),
                request.getEventType(),
                request.getOrganizerId(),
                request.getVenueId(),
                request.getStartDateTime()
        );

        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            List<Participant> participants = participantRepository.findAllById(request.getParticipantIds());
            if (participants.size() != request.getParticipantIds().size()) {
                throw new BusinessRuleException("Uno o más participantes especificados no existen");
            }
            event.setParticipants(new HashSet<>(participants));
        }

        Event savedEvent = eventRepository.save(event);
        return mapToEventResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(Long eventId) {
        Event event = findEventOrThrow(eventId);
        return mapToEventResponse(event);
    }

    @Override
    @Transactional
    public List<EventSectorPriceResponseDTO> setSectorPrices(Long eventId, List<EventSectorPriceRequestDTO> prices) {
        Event event = findEventOrThrow(eventId);

        if (event.getStatus() != EventStatus.BORRADOR) {
            throw new BusinessRuleException("Solo se pueden configurar precios en eventos en estado BORRADOR");
        }

        List<Sector> venueSectors = sectorRepository.findByVenueVenueId(event.getVenueId());
        Set<Long> validSectorIds = venueSectors.stream().map(Sector::getSectorId).collect(Collectors.toSet());

        for (EventSectorPriceRequestDTO priceDto : prices) {
            if (!validSectorIds.contains(priceDto.getSectorId())) {
                throw new BusinessRuleException("El sector ID " + priceDto.getSectorId() + " no pertenece al lugar asignado al evento");
            }

            EventSectorPrice sectorPrice = eventSectorPriceRepository
                    .findByEventEventIdAndSectorId(eventId, priceDto.getSectorId())
                    .orElseGet(() -> new EventSectorPrice(event, priceDto.getSectorId(), priceDto.getPrice()));

            sectorPrice.setPrice(priceDto.getPrice());
            event.addSectorPrice(sectorPrice);
        }

        eventRepository.save(event);

        return event.getSectorPrices().stream()
                .map(p -> new EventSectorPriceResponseDTO(p.getSectorId(), p.getPrice()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventResponseDTO publishEvent(Long eventId) {
        Event event = findEventOrThrow(eventId);

        // Validar Máquina de Estados: BORRADOR -> PUBLICADO
        if (event.getStatus() != EventStatus.BORRADOR) {
            throw new BusinessRuleException("Transición inválida: Solo los eventos en estado BORRADOR pueden ser publicados. Estado actual: " + event.getStatus());
        }

        // Regla de Negocio: Todos los sectores del lugar deben tener precio asignado
        List<Sector> venueSectors = sectorRepository.findByVenueVenueId(event.getVenueId());
        Set<Long> configuredPriceSectorIds = event.getSectorPrices().stream()
                .map(EventSectorPrice::getSectorId)
                .collect(Collectors.toSet());

        List<String> unpricedSectors = venueSectors.stream()
                .filter(s -> !configuredPriceSectorIds.contains(s.getSectorId()))
                .map(Sector::getName)
                .collect(Collectors.toList());

        if (!unpricedSectors.isEmpty()) {
            throw new BusinessRuleException("No se puede publicar el evento. Faltan configurar precios para los siguientes sectores: " + unpricedSectors);
        }

        event.setStatus(EventStatus.PUBLICADO);
        return mapToEventResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponseDTO cancelEvent(Long eventId, String userRole) {
        // Validación de permisos en Entrega 1 (vía encabezado X-User-Role)
        if (userRole == null || !userRole.equalsIgnoreCase("ADMIN")) {
            throw new BusinessRuleException("Permiso denegado: Solo los usuarios Administradores (X-User-Role: ADMIN) pueden cancelar eventos");
        }

        Event event = findEventOrThrow(eventId);

        // Validar Máquina de Estados: Transiciones válidas a CANCELADO
        if (event.getStatus() == EventStatus.CANCELADO) {
            throw new BusinessRuleException("El evento ya se encuentra cancelado");
        }
        if (event.getStatus() == EventStatus.FINALIZADO) {
            throw new BusinessRuleException("Transición inválida: No se puede cancelar un evento que ya ha FINALIZADO");
        }

        event.setStatus(EventStatus.CANCELADO);
        return mapToEventResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponseDTO suspendOrUpdateStatus(Long eventId, String newStatusStr, String userRole) {
        Event event = findEventOrThrow(eventId);
        EventStatus newStatus;

        try {
            newStatus = EventStatus.valueOf(newStatusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Estado no permitido: " + newStatusStr);
        }

        validateStateTransition(event.getStatus(), newStatus);
        event.setStatus(newStatus);
        return mapToEventResponse(eventRepository.save(event));
    }

    /**
     * Validador estricto de la máquina de estados.
     */
    private void validateStateTransition(EventStatus currentStatus, EventStatus newStatus) {
        if (currentStatus == newStatus) return;

        switch (currentStatus) {
            case BORRADOR -> {
                if (newStatus != EventStatus.PUBLICADO && newStatus != EventStatus.CANCELADO) {
                    throw new BusinessRuleException("Un evento en BORRADOR solo puede pasar a PUBLICADO o CANCELADO");
                }
            }
            case PUBLICADO -> {
                if (newStatus != EventStatus.AGOTADO && newStatus != EventStatus.CANCELADO && newStatus != EventStatus.FINALIZADO) {
                    throw new BusinessRuleException("Un evento PUBLICADO solo puede pasar a AGOTADO, CANCELADO o FINALIZADO");
                }
            }
            case AGOTADO -> {
                if (newStatus != EventStatus.PUBLICADO && newStatus != EventStatus.CANCELADO && newStatus != EventStatus.FINALIZADO) {
                    throw new BusinessRuleException("Un evento AGOTADO solo puede reabrirse a PUBLICADO, CANCELADO o FINALIZADO");
                }
            }
            case CANCELADO, FINALIZADO ->
                    throw new BusinessRuleException("El evento está en estado terminal (" + currentStatus + ") y no admite más cambios de estado");
        }
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con el ID: " + eventId));
    }

    private EventResponseDTO mapToEventResponse(Event event) {
        EventResponseDTO dto = new EventResponseDTO();
        dto.setEventId(event.getEventId());
        dto.setName(event.getName());
        dto.setDescription(event.getDescription());
        dto.setEventType(event.getEventType());
        dto.setOrganizerId(event.getOrganizerId());
        dto.setVenueId(event.getVenueId());
        dto.setStartDateTime(event.getStartDateTime());
        dto.setStatus(event.getStatus());

        dto.setParticipantIds(event.getParticipants().stream()
                .map(Participant::getParticipantId)
                .collect(Collectors.toSet()));

        dto.setSectorPrices(event.getSectorPrices().stream()
                .map(p -> new EventSectorPriceResponseDTO(p.getSectorId(), p.getPrice()))
                .collect(Collectors.toList()));

        return dto;
    }
}
