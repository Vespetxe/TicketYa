
package ar.edu.unicen.grupo8.comprasservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CrearCompraDTO {

    @NotNull
    @Positive
    private Long clienteId;

    @NotNull
    @Positive
    private Long eventoId;

    @NotNull
    @Positive
    private Long reservaId;


    @NotEmpty
    private List<@NotNull @Valid DetalleCompraDTO> detalles;

   public CrearCompraDTO() {
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

    public List<DetalleCompraDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompraDTO> detalles) {
        this.detalles = detalles;
    }
}
