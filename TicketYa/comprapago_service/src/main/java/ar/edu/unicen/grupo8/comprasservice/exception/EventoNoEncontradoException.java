package ar.edu.unicen.grupo8.comprasservice.exception;
public class EventoNoEncontradoException extends RuntimeException {
    public EventoNoEncontradoException(Long id) { super("No existe un evento con ID: " + id); }
}
