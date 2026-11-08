package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.domain.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record TicketStatusUpdateRequest(@NotBlank String orderId, @NotNull TicketStatus status) {}
