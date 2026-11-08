package example.microservicio_inventario_reservas.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DisponibilidadResponse {

    private Long idEvento;
    private Long idSector;
    private boolean esNumerado;
    private Integer capacidadTotal;
    private Integer disponibles;
    private List<DetalleAsientoResponse> asientosLibres; // Lista asientos numerados y con estado 'LIBRE'
}
