package grupo22.eventocatalogo_service.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(
        name = "seat",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sector_row_seat", columnNames = {"sector_id", "row_label", "seat_number"})
        }
)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_sector"))
    private Sector sector;

    @NotBlank(message = "La etiqueta de fila es obligatoria")
    @Size(max = 10, message = "La etiqueta de fila no puede superar los 10 caracteres")
    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @NotNull(message = "El número de asiento es obligatorio")
    @Min(value = 1, message = "El número de asiento debe ser positivo")
    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    public Seat() {
    }

    public Seat(Sector sector, String rowLabel, Integer seatNumber) {
        this.sector = sector;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
    }

    // Getters y Setters
    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
}