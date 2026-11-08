
package ar.edu.unicen.grupo8.comprasservice.entity;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoReintegro;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reintegros",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reintegro_clave_idempotencia",
                        columnNames = "clave_idempotencia"
                )
        }
)
public class Reintegro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucion_id")
    private Devolucion devolucion;

    @Column(name = "clave_idempotencia", nullable = false)
    private String claveIdempotencia;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReintegro estado;

    private LocalDateTime fechaProcesamiento;

    public Reintegro() {
        this.estado = EstadoReintegro.PENDIENTE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pago getPago() {
        return pago;
    }

    public void setPago(Pago pago) {
        this.pago = pago;
    }

    public Devolucion getDevolucion() {
        return devolucion;
    }

    public void setDevolucion(Devolucion devolucion) {
        this.devolucion = devolucion;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public EstadoReintegro getEstado() {
        return estado;
    }


    public void setEstado(EstadoReintegro nuevoEstado) {
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "Transicion de estado invalida: "
                            + this.estado + " -> " + nuevoEstado
            );
        }

        this.estado = nuevoEstado;
    }


    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    private String referenciaExterna;
    public String getReferenciaExterna() { return referenciaExterna; }
    public void setReferenciaExterna(String referencia) { referenciaExterna = referencia; }

}
