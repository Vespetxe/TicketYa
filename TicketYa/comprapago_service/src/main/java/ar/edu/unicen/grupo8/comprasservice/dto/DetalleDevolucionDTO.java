package ar.edu.unicen.grupo8.comprasservice.dto;

import java.math.BigDecimal;

public record DetalleDevolucionDTO(Long id, Long detalleCompraId, Long entradaId, BigDecimal importe) { }
