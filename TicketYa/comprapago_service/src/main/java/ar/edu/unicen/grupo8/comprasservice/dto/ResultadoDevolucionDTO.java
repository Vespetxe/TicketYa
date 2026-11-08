package ar.edu.unicen.grupo8.comprasservice.dto;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaDevolucion;
/** ERROR_PENDIENTE requiere reintentar con la misma clave. */
public record ResultadoDevolucionDTO(DevolucionDTO devolucion, ReintegroDTO reintegro,
        EtapaDevolucion etapa, boolean cancelacionEvento) { }
