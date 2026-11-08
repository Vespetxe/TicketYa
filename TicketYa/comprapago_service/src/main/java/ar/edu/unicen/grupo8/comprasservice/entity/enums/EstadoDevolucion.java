
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

public enum EstadoDevolucion {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    PROCESANDO,
    COMPLETADA,
    ERROR_PENDIENTE;

    public boolean puedeTransicionarA(EstadoDevolucion nuevoEstado) {
        if (nuevoEstado == null) {
            return false;
        }

        return switch (this) {
            case PENDIENTE ->
                    nuevoEstado == APROBADA
                            || nuevoEstado == RECHAZADA;

            case APROBADA ->
                    nuevoEstado == PROCESANDO;

            case PROCESANDO ->
                    nuevoEstado == COMPLETADA
                            || nuevoEstado == ERROR_PENDIENTE;

            case ERROR_PENDIENTE ->
                    nuevoEstado == PROCESANDO
                            || nuevoEstado == COMPLETADA;

            case RECHAZADA, COMPLETADA -> false;
        };
    }
}
