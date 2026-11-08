package grupo22.eventocatalogo_service.DTO.request;

import java.time.OffsetDateTime;

public class EventSearchFilterDTO {

    private String type;
    private Long organizerId;
    private Long participantId;
    private String city;
    private OffsetDateTime fromDate;
    private OffsetDateTime toDate;

    public EventSearchFilterDTO() {}

    public EventSearchFilterDTO(String type, Long organizerId, Long participantId, String city, OffsetDateTime fromDate, OffsetDateTime toDate) {
        this.type = type;
        this.organizerId = organizerId;
        this.participantId = participantId;
        this.city = city;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public OffsetDateTime getFromDate() { return fromDate; }
    public void setFromDate(OffsetDateTime fromDate) { this.fromDate = fromDate; }

    public OffsetDateTime getToDate() { return toDate; }
    public void setToDate(OffsetDateTime toDate) { this.toDate = toDate; }
}