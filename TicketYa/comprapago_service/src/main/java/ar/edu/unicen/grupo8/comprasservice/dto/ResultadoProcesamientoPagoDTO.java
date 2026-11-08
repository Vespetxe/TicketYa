
package ar.edu.unicen.grupo8.comprasservice.dto;

import ar.edu.unicen.grupo8.comprasservice.entity.Pago;

public record ResultadoProcesamientoPagoDTO(
        Pago pago,
        boolean creado
) {
}
