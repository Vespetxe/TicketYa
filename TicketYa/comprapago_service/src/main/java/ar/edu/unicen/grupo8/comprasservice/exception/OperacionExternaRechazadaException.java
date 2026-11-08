package ar.edu.unicen.grupo8.comprasservice.exception;

/** Rechazo definitivo: el cliente garantiza que no ejecutó el efecto solicitado. */
public class OperacionExternaRechazadaException extends IllegalStateException {
    public OperacionExternaRechazadaException(String mensaje) { super(mensaje); }
}
