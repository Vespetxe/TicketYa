package ar.edu.unicen.grupo8.comprasservice.dto;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoReintegro;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ReintegroDTO(Long id, Long pagoId, Long devolucionId, String claveIdempotencia,
        BigDecimal importe, EstadoReintegro estado, LocalDateTime fechaProcesamiento, String referenciaExterna) { }
