
package ar.edu.unicen.grupo8.comprasservice.exception;

public class PagoNoEncontradoException extends RuntimeException {

    public PagoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
