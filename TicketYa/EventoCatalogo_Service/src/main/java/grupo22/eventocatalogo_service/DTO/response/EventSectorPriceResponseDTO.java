package grupo22.eventocatalogo_service.DTO.response;

import java.math.BigDecimal;

public class EventSectorPriceResponseDTO {

    private Long sectorId;
    private BigDecimal price;

    public EventSectorPriceResponseDTO() {}

    public EventSectorPriceResponseDTO(Long sectorId, BigDecimal price) {
        this.sectorId = sectorId;
        this.price = price;
    }

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}