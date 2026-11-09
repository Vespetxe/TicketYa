package example.microservicio_inventario_reservas.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AsientoDTO {

    private Long catalogoAsientoId;

    public AsientoDTO(String fila, Integer numero) { this.fila=fila; this.numero=numero; }

    private String fila;
    private Integer numero;
}
