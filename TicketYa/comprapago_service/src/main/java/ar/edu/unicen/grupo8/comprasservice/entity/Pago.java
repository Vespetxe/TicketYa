
package ar.edu.unicen.grupo8.comprasservice.entity;

import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoPago;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "pagos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pago_clave_idempotencia",
                        columnNames = "clave_idempotencia"
                )
        }
)
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(name = "clave_idempotencia", nullable = false)
    private String claveIdempotencia;

    // Nullable exclusivamente para pagos históricos cuyo medio no fue registrado.
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_pago", length = 32)
    private ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago medioPago;

    public ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago getMedioPago() { return medioPago; }
    public void setMedioPago(ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago medioPago) { this.medioPago = medioPago; }

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importeReintegrado = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaProcesamiento;

    private String referenciaExterna;

    private String referenciaReintegro;

    public Pago() {
        this.estado = EstadoPago.PENDIENTE;
        this.fechaCreacion = LocalDateTime.now(java.time.Clock.systemUTC());
        this.importeReintegrado = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
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

    public BigDecimal getImporteReintegrado() {
        return importeReintegrado;
    }

    public void setImporteReintegrado(BigDecimal importeReintegrado) {
        this.importeReintegrado = importeReintegrado;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public void setEstado(EstadoPago nuevoEstado) {
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "Transicion de estado invalida: "
                            + this.estado + " -> " + nuevoEstado
            );
        }

        this.estado = nuevoEstado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public String getReferenciaExterna() {
        return referenciaExterna;
    }

    public void setReferenciaExterna(String referenciaExterna) {
        this.referenciaExterna = referenciaExterna;
    }

    public String getReferenciaReintegro() {
        return referenciaReintegro;
    }

    public void setReferenciaReintegro(String referenciaReintegro) {
        this.referenciaReintegro = referenciaReintegro;
    }
}
