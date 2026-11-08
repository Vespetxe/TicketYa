package ar.edu.tudai.ticketya.servicio_entradas.api;
import jakarta.validation.constraints.NotBlank;
public record IssueTicketsRequest(@NotBlank String orderId) {}
