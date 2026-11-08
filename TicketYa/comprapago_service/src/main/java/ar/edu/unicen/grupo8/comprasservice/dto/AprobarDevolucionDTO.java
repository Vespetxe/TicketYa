package ar.edu.unicen.grupo8.comprasservice.dto;

import jakarta.validation.constraints.*;

public record AprobarDevolucionDTO(@NotBlank @Size(max = 255) String motivo) { }
