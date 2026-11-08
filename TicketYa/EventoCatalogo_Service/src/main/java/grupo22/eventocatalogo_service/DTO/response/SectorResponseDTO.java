package grupo22.eventocatalogo_service.DTO.response;

import grupo22.eventocatalogo_service.domain.SectorType;

public class SectorResponseDTO {

    private Long sectorId;
    private Long venueId;
    private String name;
    private SectorType type;
    private Integer capacity;
    private Integer totalSeatsGenerated;

    public SectorResponseDTO() {}

    public SectorResponseDTO(Long sectorId, Long venueId, String name, SectorType type, Integer capacity, Integer totalSeatsGenerated) {
        this.sectorId = sectorId;
        this.venueId = venueId;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.totalSeatsGenerated = totalSeatsGenerated;
    }

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }

    public Long getVenueId() { return venueId; }
    public void setVenueId(Long venueId) { this.venueId = venueId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SectorType getType() { return type; }
    public void setType(SectorType type) { this.type = type; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getTotalSeatsGenerated() { return totalSeatsGenerated; }
    public void setTotalSeatsGenerated(Integer totalSeatsGenerated) { this.totalSeatsGenerated = totalSeatsGenerated; }
}