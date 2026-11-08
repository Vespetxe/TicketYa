package grupo22.eventocatalogo_service.controller;
import grupo22.eventocatalogo_service.DTO.request.EventRequestDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSectorPriceRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.EventResponseDTO;
import grupo22.eventocatalogo_service.DTO.response.EventSectorPriceResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import grupo22.eventocatalogo_service.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Eventos", description = "Endpoints para la creación, configuración de precios y control del ciclo de vida de eventos")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Crear un evento en borrador", description = "Registra un nuevo evento asociándole un lugar (venueId), fecha futura y lista de participantes. El evento inicia en estado BORRADOR.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Evento creado exitosamente en estado BORRADOR",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida (ej. fecha pasada o campos requeridos faltantes)",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":400,\"errors\":{\"startDateTime\":\"La fecha de inicio debe ser en el futuro\"}}"))),
            @ApiResponse(responseCode = "404", description = "El lugar (venueId) especificado no existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"El lugar con ID 99 no existe\"}"))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio no cumplida (ej. participantes especificados no existen)",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":422,\"message\":\"Uno o más participantes especificados no existen\"}")))
    })
    public ResponseEntity<EventResponseDTO> createEvent(@Valid @RequestBody EventRequestDTO request) {
        EventResponseDTO response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Obtener detalle completo de un evento por ID", description = "Retorna la información básica, estado actual, participantes y la matriz de precios por sector del evento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Evento no encontrado con el ID: 10\"}")))
    })
    public ResponseEntity<EventResponseDTO> getEventByIdgetEventById(
            @Parameter(description = "ID del evento a consultar", example = "10") @PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @PostMapping("/{eventId}/prices")
    @Operation(summary = "Configurar precios por sector para un evento", description = "Permite asignar tarifas a cada sector del lugar asignado al evento. Solo se permite en eventos en estado BORRADOR.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Matriz de precios guardada exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EventSectorPriceResponseDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Error de validación (ej. precio negativo o igual a 0)",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":400,\"errors\":{\"price\":\"El precio debe ser estrictamente mayor a 0\"}}"))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Evento no encontrado con el ID: 10\"}"))),
            @ApiResponse(responseCode = "422", description = "Inconsistencia (ej. intentar modificar precios en un evento publicado o sector no pertenece al lugar)",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":422,\"message\":\"Solo se pueden configurar precios en eventos en estado BORRADOR\"}")))
    })
    public ResponseEntity<List<EventSectorPriceResponseDTO>> setSectorPrices(
            @Parameter(description = "ID del evento a configurar", example = "10") @PathVariable Long eventId,
            @Valid @RequestBody List<EventSectorPriceRequestDTO> prices) {
        List<EventSectorPriceResponseDTO> response = eventService.setSectorPrices(eventId, prices);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{eventId}/publish")
    @Operation(summary = "Publicar evento para la venta", description = "Cambia el estado del evento de BORRADOR a PUBLICADO. Requiere que todos los sectores del lugar tengan un precio configurado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento publicado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Evento no encontrado con el ID: 10\"}"))),
            @ApiResponse(responseCode = "409", description = "Conflicto de estado (ej. el evento no está en BORRADOR)",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":409,\"message\":\"Transición inválida: Solo los eventos en estado BORRADOR pueden ser publicados. Estado actual: PUBLICADO\"}"))),
            @ApiResponse(responseCode = "422", description = "No se cumplen las precondiciones (ej. faltan precios en sectores)",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":422,\"message\":\"No se puede publicar el evento. Faltan configurar precios para los siguientes sectores: [Campo, Platea]\"}")))
    })
    public ResponseEntity<EventResponseDTO> publishEvent(
            @Parameter(description = "ID del evento a publicar", example = "10") @PathVariable Long eventId) {
        EventResponseDTO response = eventService.publishEvent(eventId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{eventId}/cancel")
    @Operation(summary = "Cancelar evento (Exclusivo Administradores)", description = "Transiciona el estado del evento a CANCELADO. Requiere identificarse con el encabezado X-User-Role: ADMIN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Evento cancelado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Evento no encontrado con el ID: 10\"}"))),
            @ApiResponse(responseCode = "409", description = "Transición inválida o permiso denegado por rol o estado terminal",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":409,\"message\":\"Permiso denegado: Solo los usuarios Administradores (X-User-Role: ADMIN) pueden cancelar eventos\"}")))
    })
    public ResponseEntity<EventResponseDTO> cancelEvent(
            @Parameter(description = "ID del evento a cancelar", example = "10") @PathVariable Long eventId,
            @Parameter(description = "Rol del usuario autenticado (requerido ADMIN)", example = "ADMIN")
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        EventResponseDTO response = eventService.cancelEvent(eventId, userRole);
        return ResponseEntity.ok(response);
    }
}