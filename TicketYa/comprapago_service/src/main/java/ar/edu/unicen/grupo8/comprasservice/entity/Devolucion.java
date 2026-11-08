
package ar.edu.unicen.grupo8.comprasservice.entity;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "devoluciones")
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDevolucion estado;

    @Column(nullable = false)
    private String motivo;

    private String motivoRechazo;

    private String motivoResolucion;
    private LocalDateTime fechaResolucion;

    public String getMotivoResolucion() { return motivoResolucion; }
    public void setMotivoResolucion(String motivo) { this.motivoResolucion = motivo; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fecha) { this.fechaResolucion = fecha; }

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importeSolicitado;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importeReintegrado;

    public Devolucion() {
        this.fechaSolicitud = LocalDateTime.now(java.time.Clock.systemUTC());
        this.estado = EstadoDevolucion.PENDIENTE;
        this.importeSolicitado = BigDecimal.ZERO;
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

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }


    public void setEstado(EstadoDevolucion nuevoEstado) {
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "Transicion de estado invalida: "
                            + this.estado + " -> " + nuevoEstado
            );
        }

        this.estado = nuevoEstado;
    }


    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public BigDecimal getImporteSolicitado() {
        return importeSolicitado;
    }

    public void setImporteSolicitado(BigDecimal importeSolicitado) {
        this.importeSolicitado = importeSolicitado;
    }

    public BigDecimal getImporteReintegrado() {
        return importeReintegrado;
    }

    public void setImporteReintegrado(BigDecimal importeReintegrado) {
        this.importeReintegrado = importeReintegrado;
    }

    @Enumerated(EnumType.STRING)
    private ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaDevolucion etapa =
            ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaDevolucion.REINTEGRO;
    private boolean cancelacionEvento;
    public ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaDevolucion getEtapa() { return etapa; }
    public void setEtapa(ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaDevolucion etapa) { this.etapa = etapa; }
    public boolean isCancelacionEvento() { return cancelacionEvento; }
    public void setCancelacionEvento(boolean valor) { cancelacionEvento = valor; }

}
