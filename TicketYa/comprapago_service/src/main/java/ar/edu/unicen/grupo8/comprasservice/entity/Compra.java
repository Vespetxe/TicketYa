
package ar.edu.unicen.grupo8.comprasservice.entity;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoCompra;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaCompra;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compras", uniqueConstraints = @UniqueConstraint(name = "uk_compra_reserva", columnNames = "reserva_id"))
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private Long eventoId;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCompra estado;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtapaCompra etapa = EtapaCompra.PAGO;

    private String ejecutor;
    private LocalDateTime bloqueoHasta;
    private LocalDateTime reservaVencimiento;

    @ElementCollection
    @CollectionTable(name = "compra_entradas", joinColumns = @JoinColumn(name = "compra_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = "entrada_id"))
    @Column(name = "entrada_id", nullable = false)
    private List<Long> entradasIds = new ArrayList<>();

    public EtapaCompra getEtapa() { return etapa; }
    public void setEtapa(EtapaCompra etapa) { this.etapa = etapa; }
    public String getEjecutor() { return ejecutor; }
    public void setEjecutor(String ejecutor) { this.ejecutor = ejecutor; }
    public LocalDateTime getBloqueoHasta() { return bloqueoHasta; }
    public void setBloqueoHasta(LocalDateTime hasta) { this.bloqueoHasta = hasta; }
    public List<Long> getEntradasIds() { return entradasIds; }
    public LocalDateTime getReservaVencimiento() { return reservaVencimiento; }
    public void setReservaVencimiento(LocalDateTime fecha) { this.reservaVencimiento = fecha; }

    @OneToMany(
            mappedBy = "compra",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DetalleCompra> detalles = new ArrayList<>();


    public Compra() {
        this.fechaCreacion = LocalDateTime.now(java.time.Clock.systemUTC());
        this.estado = EstadoCompra.PENDIENTE;
        this.importeTotal = BigDecimal.ZERO;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public void setEventoId(Long eventoId) {
        this.eventoId = eventoId;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public void setReservaId(Long reservaId) {
        this.reservaId = reservaId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

    public EstadoCompra getEstado() {
        return estado;
    }


    public void setEstado(EstadoCompra nuevoEstado) {
        if (!this.estado.puedeTransicionarA(nuevoEstado)) {
            throw new IllegalArgumentException(
                    "Transicion de estado invalida: "
                            + this.estado + " -> " + nuevoEstado
            );
        }

        this.estado = nuevoEstado;
    }


    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles;
    }


    public void agregarDetalle(DetalleCompra detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }

        detalles.add(detalle);
        detalle.setCompra(this);
    }


}
