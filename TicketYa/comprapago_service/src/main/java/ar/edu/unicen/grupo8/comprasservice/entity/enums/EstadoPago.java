
package ar.edu.unicen.grupo8.comprasservice.entity.enums;

public enum EstadoPago {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    RESULTADO_DESCONOCIDO,
    REINTEGRO_DESCONOCIDO,
    REINTEGRADO_PARCIAL,
    REINTEGRADO_TOTAL;

    public boolean puedeTransicionarA(EstadoPago nuevoEstado) {
        if (nuevoEstado == null) {
            return false;
        }

        return switch (this) {

            case PENDIENTE ->
                    nuevoEstado == APROBADO
                            || nuevoEstado == RECHAZADO
                            || nuevoEstado == RESULTADO_DESCONOCIDO;

            case RESULTADO_DESCONOCIDO ->
                    nuevoEstado == APROBADO
                            || nuevoEstado == RECHAZADO;

            case APROBADO ->
                    nuevoEstado == REINTEGRO_DESCONOCIDO
                            || nuevoEstado == REINTEGRADO_PARCIAL
                            || nuevoEstado == REINTEGRADO_TOTAL;

            case REINTEGRO_DESCONOCIDO ->
                    nuevoEstado == APROBADO
                            || nuevoEstado == REINTEGRADO_PARCIAL
                            || nuevoEstado == REINTEGRADO_TOTAL;

            case REINTEGRADO_PARCIAL ->
                    nuevoEstado == REINTEGRADO_TOTAL;

            case RECHAZADO, REINTEGRADO_TOTAL -> false;
        };
    }
}
