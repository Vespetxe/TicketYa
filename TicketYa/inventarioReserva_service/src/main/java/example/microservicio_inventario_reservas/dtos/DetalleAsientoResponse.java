package example.microservicio_inventario_reservas.dtos;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DetalleAsientoResponse {

    private Long idAsiento;
    private String fila;
    private Integer numero;
}
