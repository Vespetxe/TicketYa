package ar.edu.tudai.ticketya.servicio_entradas.api;
public class InvalidTicketQueryException extends RuntimeException {
    public InvalidTicketQueryException() { super("Provide exactly one of orderId or clientId"); }
}
