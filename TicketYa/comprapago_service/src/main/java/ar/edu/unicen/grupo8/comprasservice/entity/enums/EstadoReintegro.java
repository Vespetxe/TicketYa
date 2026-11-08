
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

public enum EstadoReintegro {
    PENDIENTE,
    COMPLETADO,
    RECHAZADO,
    RESULTADO_DESCONOCIDO;

    public boolean puedeTransicionarA(EstadoReintegro nuevoEstado) {
        if (nuevoEstado == null) {
            return false;
        }

        return switch (this) {
            case PENDIENTE ->
                    nuevoEstado == COMPLETADO
                            || nuevoEstado == RECHAZADO
                            || nuevoEstado == RESULTADO_DESCONOCIDO;

            case RESULTADO_DESCONOCIDO ->
                    nuevoEstado == COMPLETADO
                            || nuevoEstado == RECHAZADO;

            case COMPLETADO, RECHAZADO -> false;
        };
    }
}
