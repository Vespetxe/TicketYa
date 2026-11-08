package grupo22.eventocatalogo_service.DTO.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class VenueResponseDTO {

    private Long venueId;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private OffsetDateTime createdAt;

    public VenueResponseDTO() {}

    public VenueResponseDTO(Long venueId, String name, String address, BigDecimal latitude, BigDecimal longitude, OffsetDateTime createdAt) {
        this.venueId = venueId;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
    }

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}