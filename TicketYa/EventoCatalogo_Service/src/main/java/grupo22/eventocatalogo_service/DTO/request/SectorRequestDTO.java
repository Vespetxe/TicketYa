package grupo22.eventocatalogo_service.DTO.request;

import grupo22.eventocatalogo_service.domain.SectorType;
import jakarta.validation.constraints.*;

public class SectorRequestDTO {

    @NotBlank(message = "El nombre del sector no puede estar vacío")
    @Size(max = 100, message = "El nombre del sector no puede superar los 100 caracteres")
    private String name;

    @NotNull(message = "El tipo de sector es obligatorio (NUMERADO o NO_NUMERADO)")
    private SectorType type;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    private Integer capacity;

    // Campos condicionales necesarios si el tipo es NUMERADO
    @Min(value = 1, message = "Las filas deben ser al menos 1 para sectores numerados")
    private Integer rows;

    @Min(value = 1, message = "Los asientos por fila deben ser al menos 1 para sectores numerados")
    private Integer seatsPerRow;

    public SectorRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public SectorType getType() { return type; }
    public void setType(SectorType type) { this.type = type; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getRows() { return rows; }
    public void setRows(Integer rows) { this.rows = rows; }

    public Integer getSeatsPerRow() { return seatsPerRow; }
    public void setSeatsPerRow(Integer seatsPerRow) { this.seatsPerRow = seatsPerRow; }
}