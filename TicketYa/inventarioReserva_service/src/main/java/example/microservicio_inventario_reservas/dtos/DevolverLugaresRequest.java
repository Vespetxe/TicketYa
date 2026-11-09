package example.microservicio_inventario_reservas.dtos;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DevolverLugaresRequest(@NotEmpty List<@NotNull @Valid Unidad> entradas) {
    public record Unidad(@NotBlank @Size(max=64) String entradaId, Long asientoId) {}
}
