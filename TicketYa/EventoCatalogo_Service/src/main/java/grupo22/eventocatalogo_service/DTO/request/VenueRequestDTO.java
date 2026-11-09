package grupo22.eventocatalogo_service.DTO.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class VenueRequestDTO {
    private String city;
    public String getCity() {return city;}
    public void setCity(String city) {this.city=city;}

    @NotBlank(message = "El nombre del lugar no puede estar vacío")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String name;

    @NotBlank(message = "La dirección no puede estar vacía")
    @Size(max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String address;

    @NotNull(message = "La latitud es obligatoria")
    @DecimalMin(value = "-90.0", message = "La latitud mínima permitida es -90.0")
    @DecimalMax(value = "90.0", message = "La latitud máxima permitida es 90.0")
    private BigDecimal latitude;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "-180.0", message = "La longitud mínima permitida es -180.0")
    @DecimalMax(value = "180.0", message = "La longitud máxima permitida es 180.0")
    private BigDecimal longitude;

    public VenueRequestDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
}
