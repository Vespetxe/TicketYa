package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.api.TicketResponse;
import java.util.List;
public record TicketIssuanceResult(List<TicketResponse> tickets, boolean created) {}
