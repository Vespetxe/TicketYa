package grupo22.eventocatalogo_service.DTO.response;

public class SeatResponseDTO {

    private Long seatId;
    private String rowLabel;
    private Integer seatNumber;

    public SeatResponseDTO() {}

    public SeatResponseDTO(Long seatId, String rowLabel, Integer seatNumber) {
        this.seatId = seatId;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
    }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public String getRowLabel() { return rowLabel; }
    public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }

    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
}
