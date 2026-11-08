
package ar.edu.unicen.grupo8.comprasservice.dto;

import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoCompra;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CompraDTO {

    private Long id;
    private Long clienteId;
    private Long eventoId;
    private Long reservaId;
    private LocalDateTime fechaCreacion;
    private BigDecimal importeTotal;
    private EstadoCompra estado;
    private List<DetalleCompraRespuestaDTO> detalles;
    private ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaCompra etapa;
    private List<Long> entradasIds;

    public ar.edu.unicen.grupo8.comprasservice.entity.enums.EtapaCompra getEtapa() { return etapa; }
    public List<Long> getEntradasIds() { return entradasIds; }

    public static CompraDTO desde(ar.edu.unicen.grupo8.comprasservice.entity.Compra c) {
        CompraDTO dto = new CompraDTO();
        dto.id = c.getId(); dto.clienteId = c.getClienteId(); dto.eventoId = c.getEventoId();
        dto.reservaId = c.getReservaId(); dto.fechaCreacion = c.getFechaCreacion();
        dto.importeTotal = c.getImporteTotal(); dto.estado = c.getEstado(); dto.etapa = c.getEtapa();
        dto.entradasIds = List.copyOf(c.getEntradasIds());
        dto.detalles = c.getDetalles().stream().map(d -> {
            DetalleCompraRespuestaDTO detalle = new DetalleCompraRespuestaDTO();
            detalle.setId(d.getId()); detalle.setSectorId(d.getSectorId()); detalle.setAsientoId(d.getAsientoId());
            detalle.setCantidad(d.getCantidad()); detalle.setPrecioUnitario(d.getPrecioUnitario()); detalle.setSubtotal(d.getSubtotal());
            return detalle;
        }).toList();
        return dto;
    }

    public CompraDTO() {
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

    public void setEstado(EstadoCompra estado) {
        this.estado = estado;
    }


    public List<DetalleCompraRespuestaDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompraRespuestaDTO> detalles) {
        this.detalles = detalles;
    }

}
