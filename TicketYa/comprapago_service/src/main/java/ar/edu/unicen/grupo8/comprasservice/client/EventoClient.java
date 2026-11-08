
package ar.edu.unicen.grupo8.comprasservice.client;

import java.math.BigDecimal;

public interface EventoClient {

    boolean eventoValido(Long eventoId);

    /** Fecha del inicio del evento expresada en UTC. */
    java.time.LocalDateTime obtenerFechaInicio(Long eventoId);

    BigDecimal obtenerPrecioSector(
            Long eventoId,
            Long sectorId
    );

    boolean eventoCancelado(Long eventoId);
    /** Recepción simulada de una notificación administrativa de cancelación. */
    void registrarCancelacion(Long eventoId);

}
