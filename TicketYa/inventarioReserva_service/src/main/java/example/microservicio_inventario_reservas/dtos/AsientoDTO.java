package example.microservicio_inventario_reservas.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AsientoDTO {

    private String fila;
    private Integer numero;
}
