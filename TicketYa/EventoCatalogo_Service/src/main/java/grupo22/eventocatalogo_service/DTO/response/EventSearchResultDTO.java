package grupo22.eventocatalogo_service.DTO.response;

import grupo22.eventocatalogo_service.domain.EventStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class EventSearchResultDTO {

    private Long eventId;
    private String name;
    private String eventType;
    private Long venueId;
    private OffsetDateTime startDateTime;
    private EventStatus status;
    private BigDecimal minPrice;

    public EventSearchResultDTO() {}

    public EventSearchResultDTO(Long eventId, String name, String eventType, Long venueId,
                                OffsetDateTime startDateTime, EventStatus status, BigDecimal minPrice) {
        this.eventId = eventId;
        this.name = name;
        this.eventType = eventType;
        this.venueId = venueId;
        this.startDateTime = startDateTime;
        this.status = status;
        this.minPrice = minPrice;
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public OffsetDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(OffsetDateTime startDateTime) { this.startDateTime = startDateTime; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
}
