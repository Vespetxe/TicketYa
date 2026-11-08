package ar.edu.unicen.grupo8.comprasservice.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record SolicitarDevolucionDTO(
        @NotNull @Positive Long compraId,
        @NotNull @Positive Long clienteId,
        @NotEmpty List<@NotNull @Positive Long> entradasIds,
        @NotBlank @Size(max = 255) String motivo
) { }
