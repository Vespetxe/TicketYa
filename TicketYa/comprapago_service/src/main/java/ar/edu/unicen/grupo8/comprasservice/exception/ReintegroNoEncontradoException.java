package ar.edu.unicen.grupo8.comprasservice.exception;
public class ReintegroNoEncontradoException extends RuntimeException {
    public ReintegroNoEncontradoException(Long id) { super("No existe un reintegro con ID: " + id); }
}
