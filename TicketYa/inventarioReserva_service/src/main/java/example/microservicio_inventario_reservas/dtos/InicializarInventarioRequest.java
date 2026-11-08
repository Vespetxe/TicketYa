package example.microservicio_inventario_reservas.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class InicializarInventarioRequest {

    private Long idSector;
    private boolean esNumerado;
    private Integer capacidadTotal;
    private List<AsientoDTO> asientos; // Lista inicial de los asientos, solo si es numerado
}
