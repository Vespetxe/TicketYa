package ar.edu.unicen.grupo8.comprasservice.service;

import ar.edu.unicen.grupo8.comprasservice.dto.ResultadoPasarela;
import java.math.BigDecimal;

public interface PasarelaPagoService {

    ResultadoPasarela procesarPago(
            String claveIdempotencia,
            BigDecimal importe
    );

    ResultadoPasarela consultarPago(
            String claveIdempotencia
    );

    ResultadoPasarela reintegrarPago(
            String claveIdempotencia,
            BigDecimal importe
    );

    ResultadoPasarela consultarReintegro(
            String claveIdempotencia
    );

    /** Clave del reintegro independiente del cobro. */
    ResultadoPasarela reintegrarPago(String clavePago, String claveReintegro, BigDecimal importe);
    ResultadoPasarela consultarReintegro(String clavePago, String claveReintegro);

}