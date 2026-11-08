
package ar.edu.unicen.grupo8.comprasservice.client;

import ar.edu.unicen.grupo8.comprasservice.dto.DetalleEmisionDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.EntradaDTO;

import java.util.List;

public interface EntradaClient {

    List<EntradaDTO> solicitarEntradas(
            Long compraId,
            Long eventoId,
            Long clienteId,
            List<DetalleEmisionDTO> detalles
    );

    List<EntradaDTO> consultarEntradas(Long compraId);

    void anularEntradas(Long compraId);

    /** Anulación idempotente exclusivamente de las entradas seleccionadas. */
    void anularEntradas(Long compraId, List<Long> entradasIds);

}
