
package ar.edu.unicen.grupo8.comprasservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class DetalleCompraDTO {

    @NotNull
    @Positive
    private Long sectorId;

    @Positive
    private Long asientoId;

    @NotNull
    @Positive
    private Integer cantidad;

    public DetalleCompraDTO() {
    }

    public Long getSectorId() {
        return sectorId;
    }

    public void setSectorId(Long sectorId) {
        this.sectorId = sectorId;
    }

    public Long getAsientoId() {
        return asientoId;
    }

    public void setAsientoId(Long asientoId) {
        this.asientoId = asientoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
