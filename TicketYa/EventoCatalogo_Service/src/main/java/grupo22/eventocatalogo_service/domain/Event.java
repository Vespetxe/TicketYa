package grupo22.eventocatalogo_service.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @NotBlank(message = "El nombre del evento no puede estar vacío")
    @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "El tipo de evento no puede estar vacío")
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // Recital, Teatro, Deporte, Conferencia

    @NotNull(message = "El ID del organizador es obligatorio")
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId; // Referencia externa desnormalizada

    @NotNull(message = "El ID del lugar (venue) es obligatorio")
    @Column(name = "venue_id", nullable = false)
    private Long venueId; // Referencia interna al Venue

    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    @Future(message = "La fecha de inicio del evento debe ser en el futuro")
    @Column(name = "start_date_time", nullable = false)
    private OffsetDateTime startDateTime;

    @NotNull(message = "El estado del evento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status = EventStatus.BORRADOR;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_participant",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "participant_id")
    )
    private Set<Participant> participants = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventSectorPrice> sectorPrices = new ArrayList<>();

    public Event() {}

    public Event(String name, String description, String eventType, Long organizerId, Long venueId, OffsetDateTime startDateTime) {
        this.name = name;
        this.description = description;
        this.eventType = eventType;
        this.organizerId = organizerId;
        this.venueId = venueId;
        this.startDateTime = startDateTime;
        this.status = EventStatus.BORRADOR;
    }

    // Métodos Helper
    public void addParticipant(Participant participant) {
        this.participants.add(participant);
    }

    public void addSectorPrice(EventSectorPrice price) {
        this.sectorPrices.add(price);
        price.setEvent(this);
    }

    // Getters y Setters
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

    public Set<Participant> getParticipants() { return participants; }
    public void setParticipants(Set<Participant> participants) { this.participants = participants; }

    public List<EventSectorPrice> getSectorPrices() { return sectorPrices; }
    public void setSectorPrices(List<EventSectorPrice> sectorPrices) { this.sectorPrices = sectorPrices; }
}