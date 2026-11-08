package grupo22.eventocatalogo_service.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(
        name = "event_sector_price",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_sector", columnNames = {"event_id", "sector_id"})
        }
)
public class EventSectorPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_sector_price_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_price_event"))
    private Event event;

    @NotNull(message = "El sector_id es obligatorio")
    @Column(name = "sector_id", nullable = false)
    private Long sectorId;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio del sector debe ser mayor a 0")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    public EventSectorPrice() {}

    public EventSectorPrice(Event event, Long sectorId, BigDecimal price) {
        this.event = event;
        this.sectorId = sectorId;
        this.price = price;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}