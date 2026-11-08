package grupo22.eventocatalogo_service.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "sector",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_venue_sector_name", columnNames = {"venue_id", "name"})
        }
)
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sector_id")
    private Long sectorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sector_venue"))
    private Venue venue;

    @NotBlank(message = "El nombre del sector no puede estar vacío")
    @Size(max = 100, message = "El nombre del sector no puede superar los 100 caracteres")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(message = "El tipo de sector es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SectorType type;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();

    public Sector() {
    }

    public Sector(Venue venue, String name, SectorType type, Integer capacity) {
        this.venue = venue;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    // Helper method para mantener consistencia bidireccional con los asientos
    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setSector(this);
    }

    public void removeSeat(Seat seat) {
        seats.remove(seat);
        seat.setSector(null);
    }

    // Getters y Setters
    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SectorType getType() {
        return type;
    }

    public void setType(SectorType type) {
        this.type = type;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }
}