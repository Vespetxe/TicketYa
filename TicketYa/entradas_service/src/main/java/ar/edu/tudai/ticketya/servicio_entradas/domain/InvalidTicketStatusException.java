package ar.edu.tudai.ticketya.servicio_entradas.domain;
public class InvalidTicketStatusException extends RuntimeException {
    public InvalidTicketStatusException() { super("Tickets may only be changed to ANULADA or REINTEGRADA"); }
}
