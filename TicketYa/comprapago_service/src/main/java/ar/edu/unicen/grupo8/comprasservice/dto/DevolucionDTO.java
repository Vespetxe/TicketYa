package ar.edu.unicen.grupo8.comprasservice.dto;

import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DevolucionDTO(Long id, Long compraId, Long clienteId, Long eventoId,
        LocalDateTime fechaSolicitud, EstadoDevolucion estado, String motivo,
        String motivoResolucion, String motivoRechazo, LocalDateTime fechaResolucion,
        BigDecimal importeSolicitado, BigDecimal importeReintegrado,
        List<DetalleDevolucionDTO> detalles) {
    public DevolucionDTO { detalles = List.copyOf(detalles); }
}
