
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

public enum EstadoCompra {
    PENDIENTE,
    PROCESANDO,
    COMPLETADA,
    FALLIDA,
    CANCELADA,
    DEVUELTA_PARCIAL,
    DEVUELTA_TOTAL;

    public boolean puedeTransicionarA(EstadoCompra nuevoEstado) {
        if (nuevoEstado == null) {
            return false;
        }

        return switch (this) {
            case PENDIENTE ->
                    nuevoEstado == PROCESANDO
                            || nuevoEstado == CANCELADA
                            || nuevoEstado == FALLIDA;

            case PROCESANDO ->
                    nuevoEstado == COMPLETADA
                            || nuevoEstado == FALLIDA
                            || nuevoEstado == CANCELADA;

            case COMPLETADA ->
                    nuevoEstado == DEVUELTA_PARCIAL
                            || nuevoEstado == DEVUELTA_TOTAL
                            || nuevoEstado == CANCELADA;

            case DEVUELTA_PARCIAL ->
                    nuevoEstado == DEVUELTA_TOTAL;

            case FALLIDA, CANCELADA, DEVUELTA_TOTAL -> false;
        };
    }
}
