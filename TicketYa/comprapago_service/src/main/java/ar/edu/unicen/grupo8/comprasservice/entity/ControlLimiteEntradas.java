
package ar.edu.unicen.grupo8.comprasservice.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "control_limite_entradas",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"cliente_id", "evento_id"}
        )
)
public class ControlLimiteEntradas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "evento_id", nullable = false)
    private Long eventoId;

    protected ControlLimiteEntradas() {
    }

    public ControlLimiteEntradas(Long clienteId, Long eventoId) {
        this.clienteId = clienteId;
        this.eventoId = eventoId;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getEventoId() {
        return eventoId;
    }
}
