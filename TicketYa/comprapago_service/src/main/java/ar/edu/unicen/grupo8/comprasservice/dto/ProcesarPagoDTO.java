
package ar.edu.unicen.grupo8.comprasservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProcesarPagoDTO {

    @NotNull(message = "El identificador de compra es obligatorio")
    @Positive(message = "El identificador de compra debe ser positivo")
    private Long compraId;

    @NotBlank(message = "La clave de idempotencia es obligatoria")
    @Size(max = 100)
    private String claveIdempotencia;

    @io.swagger.v3.oas.annotations.media.Schema(
            description = "Obligatorio al crear un pago. En reintentos debe coincidir con el medio original. Omitir solo al recuperar un pago historico que carece del dato.",
            example = "TARJETA_DEBITO", nullable = true)
    private ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago medioPago;

    public ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago getMedioPago() { return medioPago; }
    public void setMedioPago(ar.edu.unicen.grupo8.comprasservice.entity.enums.MedioPago medioPago) { this.medioPago = medioPago; }

    public Long getCompraId() {
        return compraId;
    }

    public void setCompraId(Long compraId) {
        this.compraId = compraId;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }
}
