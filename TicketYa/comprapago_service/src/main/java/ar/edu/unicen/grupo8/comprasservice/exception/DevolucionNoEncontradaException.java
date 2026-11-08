package ar.edu.unicen.grupo8.comprasservice.exception;

public class DevolucionNoEncontradaException extends RuntimeException {
    public DevolucionNoEncontradaException(Long id) { super("No existe una devolucion con ID: " + id); }
}
