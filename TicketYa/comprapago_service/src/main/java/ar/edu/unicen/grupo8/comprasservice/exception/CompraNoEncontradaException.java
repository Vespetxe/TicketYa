
package ar.edu.unicen.grupo8.comprasservice.exception;

public class CompraNoEncontradaException extends RuntimeException {

    public CompraNoEncontradaException(Long compraId) {
        super("No existe una compra con ID: " + compraId);
    }
}
