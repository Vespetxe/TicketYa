package example.microservicio_inventario_reservas.entities;

import example.microservicio_inventario_reservas.enums.EstadoReserva;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_reserva_cliente", columnList = "idCliente"),
        @Index(name = "idx_reserva_expiracion", columnList = "estado, fechaExpiracion")
})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;

    @Column(nullable = false)
    private Long idCliente;
    @Column(nullable = false)
    private Long idEvento;
    @Column(nullable = false)
    private Long idSector;
    @Column(nullable = false)
    private Integer cantidad;
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;
    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleReserva> detalles = new ArrayList<>();

    public void addDetalle(DetalleReserva detalle) {
        detalles.add(detalle);
        detalle.setReserva(this);
    }
}
