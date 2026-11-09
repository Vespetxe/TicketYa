package example.microservicio_inventario_reservas.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class LiberarDevolucionRequest {

    private Long idReserva;
    private List<String> entradasIds;
    public LiberarDevolucionRequest(Long idEvento, Long idSector, Integer cantidad, List<Long> idsAsientos) {
        this.idEvento=idEvento; this.idSector=idSector; this.cantidad=cantidad; this.idsAsientos=idsAsientos;
    }

    private Long idEvento;
    private Long idSector;
    private Integer cantidad;
    private List<Long> idsAsientos; // Lista asientos a liberar, solo si son numerados
}
