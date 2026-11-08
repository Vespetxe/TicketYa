package ar.edu.tudai.ticketya.servicio_entradas.domain;
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) { super(message); }
}
