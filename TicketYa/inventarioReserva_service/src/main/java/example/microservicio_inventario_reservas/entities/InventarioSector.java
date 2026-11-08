package example.microservicio_inventario_reservas.entities;

import example.microservicio_inventario_reservas.enums.EstadoInventario;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_evento_sector", columnList = "idEvento, idSector")
})
public class InventarioSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInventarioSector;

    @Column(nullable = false)
    private Long idEvento;
    @Column(nullable = false)
    private Long idSector;
    @Column(nullable = false)
    private boolean esNumerado;
    @Column(nullable = false)
    private Integer capacidadTotal;
    @Column(nullable = false)
    private Integer disponibles;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInventario estado;

    @OneToMany(mappedBy = "inventarioSector", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Asiento> asientos = new ArrayList<>();

    public void addAsiento(Asiento asiento) {
        asientos.add(asiento);
        asiento.setInventarioSector(this);
    }
}
