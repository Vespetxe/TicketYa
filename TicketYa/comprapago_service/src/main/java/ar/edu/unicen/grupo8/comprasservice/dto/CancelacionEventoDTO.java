package ar.edu.unicen.grupo8.comprasservice.dto;
import java.util.List;
public record CancelacionEventoDTO(Long eventoId, List<ResultadoDevolucionDTO> devoluciones,
        List<Long> comprasPendientes) {
    public CancelacionEventoDTO { devoluciones = List.copyOf(devoluciones); comprasPendientes = List.copyOf(comprasPendientes); }
}
