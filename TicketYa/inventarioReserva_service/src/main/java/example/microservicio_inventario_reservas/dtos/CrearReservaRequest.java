package example.microservicio_inventario_reservas.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CrearReservaRequest {

    private Long idCliente;
    private Long idEvento;
    private Long idSector;
    private Integer cantidad;
    private List<Long> idsAsientos; // Lista asientos elegidos, solo si es numerado
}
