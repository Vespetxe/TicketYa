package example.microservicio_inventario_reservas.entities;

import example.microservicio_inventario_reservas.enums.EstadoAsiento;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAsiento;

    private Long catalogoAsientoId;

    @Column(nullable = false)
    private String fila;
    @Column(nullable = false)
    private Integer numero;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAsiento estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idInventarioSector", nullable = false)
    @ToString.Exclude
    private InventarioSector inventarioSector;
}
