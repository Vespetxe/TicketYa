package example.microservicio_inventario_reservas.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MensajeResponse {

    private String mensaje;
    private boolean exitoso;
}
