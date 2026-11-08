package ar.edu.unicen.grupo8.comprasservice.dto;

public record ErrorResponse(
        int status,
        String message
) {
}
