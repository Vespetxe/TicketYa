package grupo22.eventocatalogo_service.DTO.request;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class EventSectorPriceRequestDTO {

    @NotNull(message = "El sectorId es obligatorio")
    private Long sectorId;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser estrictamente mayor a 0")
    private BigDecimal price;

    public EventSectorPriceRequestDTO() {}

    public EventSectorPriceRequestDTO(Long sectorId, BigDecimal price) {
        this.sectorId = sectorId;
        this.price = price;
    }

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
