package example.microservicio_inventario_reservas.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(name="lugares_devueltos", uniqueConstraints=@UniqueConstraint(columnNames="entrada_id"))
public class LugarDevuelto {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="entrada_id", nullable=false, length=64) private String entradaId;
    @ManyToOne(optional=false) private Reserva reserva;
    private Long asientoId;
}
