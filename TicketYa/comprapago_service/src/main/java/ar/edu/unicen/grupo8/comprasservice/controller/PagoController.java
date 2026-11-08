
package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.dto.ProcesarPagoDTO;
import ar.edu.unicen.grupo8.comprasservice.dto.PagoDTO;
import ar.edu.unicen.grupo8.comprasservice.entity.Pago;
import ar.edu.unicen.grupo8.comprasservice.service.PagoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ar.edu.unicen.grupo8.comprasservice.dto.ResultadoProcesamientoPagoDTO;
import java.util.List;

@RestController
@RequestMapping("/pagos")
@Tag(
        name = "Pagos",
        description = "API para el procesamiento de pagos de compras"
)
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @Operation(
            summary = "Procesar un pago",
            description = "Registra el intento de pago. Exige medioPago para pagos nuevos; repetir requiere la misma compra, clave y medio. Solo los pagos historicos sin medio permiten omitirlo. 201 no implica aprobacion: consultar estado del resultado."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Pago creado correctamente"
            ),
            @ApiResponse(
                    responseCode = "200",
                    description = "Pago existente recuperado por idempotencia"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos, compra o reserva incorrecta"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto con un pago existente o en procesamiento"
            )
    })
    public ResponseEntity<PagoDTO> procesarPago(
            @Valid @RequestBody ProcesarPagoDTO dto
    ) {

        ResultadoProcesamientoPagoDTO resultado =
                pagoService.procesarPagoConResultado(
                        dto.getCompraId(),
                        dto.getClaveIdempotencia(),
                        dto.getMedioPago()
                );

        if (resultado.creado()) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(PagoDTO.desde(resultado.pago()));
        }

        return ResponseEntity.ok(PagoDTO.desde(resultado.pago()));
    }


    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar un pago por ID",
            description = "Devuelve la información de un pago a partir de su identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Pago encontrado"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador inválido"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No existe un pago con el ID indicado"
            )
    })
    public ResponseEntity<PagoDTO> obtenerPagoPorId(
            @PathVariable Long id
    ) {
        Pago pago = pagoService.obtenerPagoPorId(id);

        return ResponseEntity.ok(PagoDTO.desde(pago));
    }


    @GetMapping
    @Operation(
            summary = "Consultar pagos de una compra",
            description = "Devuelve todos los pagos registrados para la compra indicada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Consulta realizada correctamente. Puede devolver una lista vacía."
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetro compraId ausente o inválido"
            )
    })
    public ResponseEntity<List<PagoDTO>> obtenerPagosPorCompra(
            @RequestParam Long compraId
    ) {
        List<Pago> pagos = pagoService.obtenerPagosPorCompra(compraId);

        return ResponseEntity.ok(pagos.stream().map(PagoDTO::desde).toList());
    }



}
