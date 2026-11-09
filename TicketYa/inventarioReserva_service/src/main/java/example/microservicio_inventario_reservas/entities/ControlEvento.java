package example.microservicio_inventario_reservas.entities;
import jakarta.persistence.*;
import example.microservicio_inventario_reservas.enums.EstadoInventario;
@Entity @Table(name="control_evento")
public class ControlEvento {
    @Id @Column(name="id_evento") private Long id;
    @Enumerated(EnumType.STRING) private EstadoInventario estado;
    public EstadoInventario getEstado(){return estado;}
    public void setEstado(EstadoInventario estado){this.estado=estado;}
}
