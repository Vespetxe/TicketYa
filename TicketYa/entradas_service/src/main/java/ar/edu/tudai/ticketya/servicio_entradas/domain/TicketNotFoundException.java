package ar.edu.tudai.ticketya.servicio_entradas.domain;
public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String ticketId) { super("Ticket not found: " + ticketId); }
}
