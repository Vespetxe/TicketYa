package grupo22.eventocatalogo_service.DTO.request;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.Set;

public class EventRequestDTO {

    @NotBlank(message = "El nombre del evento no puede estar vacío")
    @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
    private String name;

    private String description;

    @NotBlank(message = "El tipo de evento no puede estar vacío")
    private String eventType;

    @NotNull(message = "El ID del organizador es obligatorio")
    private Long organizerId;

    @NotNull(message = "El ID del lugar (venueId) es obligatorio")
    private Long venueId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser en el futuro")
    private OffsetDateTime startDateTime;

    private Set<Long> participantIds;

    public EventRequestDTO() {}

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

    public Set<Long> getParticipantIds() { return participantIds; }
    public void setParticipantIds(Set<Long> participantIds) { this.participantIds = participantIds; }
}