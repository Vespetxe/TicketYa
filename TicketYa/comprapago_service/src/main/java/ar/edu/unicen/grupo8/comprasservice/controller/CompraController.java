
package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.dto.CrearCompraDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.CompraDTO;
import ar.edu.unicen.grupo8.comprasservice.entity.Compra;
import ar.edu.unicen.grupo8.comprasservice.service.CompraService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compras")
@Tag(
        name = "Compras",
        description = "API para la creación y consulta de compras"
)
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    // Crear una compra
    @PostMapping
    @Operation(
            summary = "Crear una compra",
            description = "Valida el evento y la reserva, verifica el límite de entradas por cliente y calcula el importe total."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Compra creada correctamente"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos, reserva incorrecta o límite de entradas excedido"
            )
    })
    public ResponseEntity<CompraDTO> crearCompra(
            @Valid @RequestBody CrearCompraDTO dto
    ) {
        Compra compra = compraService.crearCompra(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(CompraDTO.desde(compra));
    }

    // Consultar una compra por ID
    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar una compra por ID",
            description = "Devuelve la información de una compra a partir de su identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Compra encontrada"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe una compra con el ID indicado"
            )
    })
    public ResponseEntity<CompraDTO> obtenerCompraPorId(
            @PathVariable Long id
    ) {
        Compra compra = compraService.obtenerCompraPorId(id);

        return ResponseEntity.ok(CompraDTO.desde(compra));
    }

    @GetMapping("/{id}/estado")
    @Operation(summary = "Consultar estado y etapa de una compra")
    public java.util.Map<String, Object> obtenerEstado(@PathVariable Long id) {
        Compra compra = compraService.obtenerCompraPorId(id);
        return java.util.Map.of("estado", compra.getEstado(), "etapa", compra.getEtapa());
    }

    @GetMapping("/{id}/detalle")
    @Operation(summary = "Consultar detalles de una compra")
    public List<ar.edu.unicen.grupo8.comprasservice.dto.DetalleCompraRespuestaDTO> obtenerDetalle(@PathVariable Long id) {
        return CompraDTO.desde(compraService.obtenerCompraPorId(id)).getDetalles();
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una compra pendiente",
            description = "Registra la cancelacion e impide nuevos pagos. Repetir recupera la liberacion de reserva pendiente sin duplicar efectos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cancelacion registrada; consultar etapa para verificar liberacion"),
            @ApiResponse(responseCode = "400", description = "Identificador invalido"),
            @ApiResponse(responseCode = "404", description = "Compra inexistente"),
            @ApiResponse(responseCode = "409", description = "Compra en procesamiento, completada o fallida")
    })
    public CompraDTO cancelarCompra(@PathVariable Long id) {
        return CompraDTO.desde(compraService.cancelarCompra(id));
    }

    // Consultar las compras de un cliente
    @GetMapping
    @Operation(
            summary = "Consultar compras de un cliente",
            description = "Devuelve todas las compras registradas para el cliente indicado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consulta realizada correctamente. Puede devolver una lista vacía."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetro clienteId ausente o inválido"
            )
    })
    public ResponseEntity<List<CompraDTO>> obtenerComprasPorCliente(
            @RequestParam Long clienteId
    ) {
        List<Compra> compras =
                compraService.obtenerComprasPorCliente(clienteId);

        return ResponseEntity.ok(compras.stream().map(CompraDTO::desde).toList());
    }
}
