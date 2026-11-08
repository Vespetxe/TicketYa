
package ar.edu.unicen.grupo8.comprasservice.dto;

public record EntradaDTO(
        Long entradaId,
        Long compraId,
        Long eventoId,
        Long clienteId,
        Long sectorId,
        Long asientoId,
        String estado,
        String codigo
) {
}
