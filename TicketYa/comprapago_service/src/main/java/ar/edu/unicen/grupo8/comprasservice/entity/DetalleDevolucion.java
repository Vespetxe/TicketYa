
package ar.edu.unicen.grupo8.comprasservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalles_devolucion")
public class DetalleDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_compra_id", nullable = false)
    private DetalleCompra detalleCompra;

    @Column(nullable = false)
    private Long entradaId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    public DetalleDevolucion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Devolucion getDevolucion() {
        return devolucion;
    }

    public void setDevolucion(Devolucion devolucion) {
        this.devolucion = devolucion;
    }

    public DetalleCompra getDetalleCompra() {
        return detalleCompra;
    }

    public void setDetalleCompra(DetalleCompra detalleCompra) {
        this.detalleCompra = detalleCompra;
    }

    public Long getEntradaId() {
        return entradaId;
    }

    public void setEntradaId(Long entradaId) {
        this.entradaId = entradaId;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }
}
