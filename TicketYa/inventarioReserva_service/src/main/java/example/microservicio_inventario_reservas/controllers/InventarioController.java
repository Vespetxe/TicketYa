package example.microservicio_inventario_reservas.controllers;

import example.microservicio_inventario_reservas.dtos.DisponibilidadResponse;
import example.microservicio_inventario_reservas.dtos.InicializarInventarioRequest;
import example.microservicio_inventario_reservas.dtos.LiberarDevolucionRequest;
import example.microservicio_inventario_reservas.dtos.MensajeResponse;
import example.microservicio_inventario_reservas.services.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Inventario", description = "Endpoints para la gestión del stock, aforos, grilla de asientos y cancelaciones del inventario de eventos")
public class InventarioController {
    @PostMapping("/internal/eventos/{id}/publicar") public void publicar(@PathVariable Long id){inventarioService.publicar(id);}
    @PostMapping("/internal/eventos/{id}/habilitar") public void habilitar(@PathVariable Long id){inventarioService.habilitar(id);}
    @PostMapping("/internal/eventos/{id}/suspender")
    public void suspender(@PathVariable Long id) {inventarioService.suspender(id);}

    @Autowired
    private InventarioService inventarioService;

    @Operation(
            summary = "Inicializar inventario de evento",
            description = "Crea la grilla inicial de asientos numerados o habilita el aforo total no numerado para un sector específico al publicar un evento."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Inventario inicializado correctamente",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El inventario para este evento y sector ya existe o los datos enviados son inválidos",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @PostMapping("/eventos/{idEvento}/inventario")
    public ResponseEntity<MensajeResponse> inicializarInventario(@PathVariable Long idEvento, @RequestBody InicializarInventarioRequest request) {
        MensajeResponse response = inventarioService.inicializarInventario(idEvento, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Consultar disponibilidad de sector",
            description = "Retorna la cantidad de cupos disponibles y la grilla de asientos libres si el sector es numerado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de disponibilidad retornado exitosamente",
                    content = @Content(schema = @Schema(implementation = DisponibilidadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Inventario no encontrado para el evento y sector especificados",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @GetMapping("/disponibilidad/eventos/{idEvento}/sectores/{idSector}")
    public ResponseEntity<DisponibilidadResponse> consultarDisponibilidad(@PathVariable Long idEvento, @PathVariable Long idSector) {
        DisponibilidadResponse response = inventarioService.consultarDisponibilidad(idEvento, idSector);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Liberar lugares por devolución",
            description = "Restablece la disponibilidad de cupos o asientos en el inventario cuando el servicio de Compras aprueba una devolución."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lugares reintegrados al inventario exitosamente",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Inventario o asientos no encontrados",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @PostMapping("/ocupaciones/liberar")
    public ResponseEntity<MensajeResponse> liberarPorDevolucion(@RequestBody LiberarDevolucionRequest request) {
        MensajeResponse response = inventarioService.liberarPorDevolucion(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancelar inventario de evento",
            description = "Invalida todo el inventario y marca como cancelados los asientos de un evento cuando es suspendido o cancelado globalmente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Inventario del evento invalidado correctamente por cancelación",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @PostMapping("/eventos/{idEvento}/cancelar-inventario")
    public ResponseEntity<MensajeResponse> cancelarInventarioEvento(@PathVariable Long idEvento) {
        MensajeResponse response = inventarioService.cancelarInventarioEvento(idEvento);
        return ResponseEntity.ok(response);
    }
}
