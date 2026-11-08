package grupo22.eventocatalogo_service.DTO.response;

import grupo22.eventocatalogo_service.domain.EventStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

public class EventResponseDTO {

    private Long eventId;
    private String name;
    private String description;
    private String eventType;
    private Long organizerId;
    private Long venueId;
    private OffsetDateTime startDateTime;
    private EventStatus status;
    private Set<Long> participantIds;
    private List<EventSectorPriceResponseDTO> sectorPrices;

    public EventResponseDTO() {}

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public OffsetDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(OffsetDateTime startDateTime) { this.startDateTime = startDateTime; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public Set<Long> getParticipantIds() { return participantIds; }
    public void setParticipantIds(Set<Long> participantIds) { this.participantIds = participantIds; }

    public List<EventSectorPriceResponseDTO> getSectorPrices() { return sectorPrices; }
    public void setSectorPrices(List<EventSectorPriceResponseDTO> sectorPrices) { this.sectorPrices = sectorPrices; }
}