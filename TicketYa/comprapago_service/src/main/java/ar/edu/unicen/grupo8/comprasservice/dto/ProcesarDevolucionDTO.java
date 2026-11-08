package ar.edu.unicen.grupo8.comprasservice.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
/** El importe debe coincidir con la totalidad autorizada de esta devolución. */
public record ProcesarDevolucionDTO(@NotBlank @Size(max=100) String claveIdempotencia,
        @NotNull @DecimalMin("0.01") @Digits(integer=10, fraction=2) BigDecimal importe) { }
