
package ar.edu.unicen.grupo8.comprasservice.client;

import ar.edu.unicen.grupo8.comprasservice.dto.ReservaDTO;

public interface ReservaClient {

    boolean reservaValida(
            Long reservaId,
            Long clienteId,
            Long eventoId
    );

    ReservaDTO consultarReserva(Long reservaId);

    void confirmarReserva(Long reservaId);

    void liberarReserva(Long reservaId);

    /** Revierte una confirmación únicamente por una compra fallida ya compensada. */
    void compensarReserva(Long reservaId);

    /** Libera solo los lugares de entradas devueltas, con identidad estable por entrada. */
    void liberarLugares(Long reservaId, java.util.List<ar.edu.unicen.grupo8.comprasservice.dto.EntradaDTO> entradas);
    java.util.List<Long> consultarLugaresLiberados(Long reservaId);

}
