package ar.edu.unicen.grupo8.comprasservice.dto;

import ar.edu.unicen.grupo8.comprasservice.entity.Pago;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoPago;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoDTO(Long id, Long compraId, String claveIdempotencia, BigDecimal importe,
        BigDecimal importeReintegrado, EstadoPago estado, LocalDateTime fechaCreacion,
        LocalDateTime fechaProcesamiento, String referenciaExterna, String referenciaReintegro,
        @io.swagger.v3.oas.annotations.media.Schema(description="Medio registrado al iniciar el pago; null si el registro historico no contiene el dato", nullable=true)
        ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago medioPago) {
    public static PagoDTO desde(Pago p) {
        return new PagoDTO(p.getId(), p.getCompra() == null ? null : p.getCompra().getId(), p.getClaveIdempotencia(),
                p.getImporte(), p.getImporteReintegrado(), p.getEstado(), p.getFechaCreacion(),
                p.getFechaProcesamiento(), p.getReferenciaExterna(), p.getReferenciaReintegro(), p.getMedioPago());
    }
}
