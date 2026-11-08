package example.microservicio_inventario_reservas.dtos;

import example.microservicio_inventario_reservas.enums.EstadoReserva;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReservaResponse {

    private Long idReserva;
    private Long idCliente;
    private Long idEvento;
    private Long idSector;
    private Integer cantidad;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private EstadoReserva estado;
    private List<DetalleAsientoResponse> asientosReservados; // Lista asientos asignados a la reserva
}
