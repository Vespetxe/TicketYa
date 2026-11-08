package example.microservicio_inventario_reservas.controllers;

import example.microservicio_inventario_reservas.dtos.CrearReservaRequest;
import example.microservicio_inventario_reservas.dtos.MensajeResponse;
import example.microservicio_inventario_reservas.dtos.ReservaResponse;
import example.microservicio_inventario_reservas.services.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservas")
@Tag(name = "Reservas", description = "Endpoints para la creación temporal con TTL, confirmación, liberación y consulta de reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Operation(
            summary = "Crear reserva temporal",
            description = "Bloquea temporalmente lugares (con TTL de 10 minutos) aplicando protección pesimista en base de datos para prevenir la doble venta."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Reserva temporal creada exitosamente",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Se ingresaron datos inválidos",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Inventario del evento/sector no encontrado",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: Asientos ya reservados/ocupados, aforo insuficiente o evento cancelado",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<ReservaResponse> crearReservaTemporal(@RequestBody CrearReservaRequest request) {
        ReservaResponse response = reservaService.crearReservaTemporal(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Consultar reserva por ID",
            description = "Obtiene los detalles completos de una reserva registrada en el sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalles de la reserva retornados exitosamente",
                    content = @Content(schema = @Schema(implementation = ReservaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> obtenerReservaPorId(@PathVariable Long id) {
        ReservaResponse response = reservaService.obtenerReservaPorId(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Consultar reservas de un cliente",
            description = "Filtra y devuelve el historial de reservas asociadas a un cliente específico."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de reservas del cliente retornado exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ReservaResponse.class)))
            )
    })
    @GetMapping
    public ResponseEntity<List<ReservaResponse>> obtenerReservasPorCliente(@RequestParam(name = "idCliente") Long idCliente) {
        List<ReservaResponse> response = reservaService.obtenerReservasPorCliente(idCliente);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Confirmar ocupación de reserva",
            description = "Confirma de forma definitiva los lugares de una reserva en estado PENDIENTE tras la aprobación del pago por el servicio de Compras."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Ocupación confirmada exitosamente",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto: La reserva no se encuentra en estado PENDIENTE (ya confirmada, expirada o liberada)",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<MensajeResponse> confirmarReserva(@PathVariable Long id) {
        MensajeResponse response = reservaService.confirmarReserva(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Liberar reserva",
            description = "Libera inmediatamente los lugares bloqueados si el pago fue rechazado o la compra fue cancelada antes de su vencimiento."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reserva liberada y disponibilidad restablecida exitosamente",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = MensajeResponse.class))
            )
    })
    @PostMapping("/{id}/liberar")
    public ResponseEntity<MensajeResponse> liberarReserva(@PathVariable Long id) {
        MensajeResponse response = reservaService.liberarReserva(id);
        return ResponseEntity.ok(response);
    }
}
