package grupo22.eventocatalogo_service.controller;

import grupo22.eventocatalogo_service.DTO.request.SectorRequestDTO;
import grupo22.eventocatalogo_service.DTO.request.VenueRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.SectorResponseDTO;
import grupo22.eventocatalogo_service.DTO.response.VenueResponseDTO;
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
import grupo22.eventocatalogo_service.service.VenueService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/venues")
@Tag(name = "Lugares y Sectores", description = "Endpoints para la gestión de infraestructura física de eventos (Estadios, Teatros, Salas)")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    @Operation(summary = "Dar de alta un nuevo lugar", description = "Crea un nuevo lugar registrando su nombre, dirección postal y coordenadas geográficas (latitud y longitud).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lugar creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VenueResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o error en coordenadas geográficas",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":400,\"errors\":{\"latitude\":\"La latitud debe ser menor o igual a 90.0\"}}")))
    })
    public ResponseEntity<VenueResponseDTO> createVenue(@Valid @RequestBody VenueRequestDTO request) {
        VenueResponseDTO response = venueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar todos los lugares", description = "Retorna el listado completo de lugares registrados en el catálogo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista devuelta exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = VenueResponseDTO.class))))
    })
    public ResponseEntity<List<VenueResponseDTO>> getAllVenues() {
        return ResponseEntity.ok(venueService.getAllVenues());
    }

    @GetMapping("/{venueId}")
    @Operation(summary = "Obtener detalle de un lugar por ID", description = "Consulta la información de un lugar específico mediante su `venueId`.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lugar encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = VenueResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Lugar no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Lugar no encontrado con el ID: 99\"}")))
    })
    public ResponseEntity<VenueResponseDTO> getVenueById(@Parameter(description = "ID del lugar a consultar", example = "1") @PathVariable Long venueId){
        return ResponseEntity.ok(venueService.getVenueById(venueId));
    }

    @PostMapping("/{venueId}/sectors")
    @Operation(summary = "Crear un sector en un lugar", description = "Asocia un sector de tipo NUMERADO (generando automáticamente su grilla de asientos) o NO_NUMERADO (aforo máximo).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sector creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SectorResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Error de sintaxis o datos requeridos faltantes",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":400,\"errors\":{\"capacity\":\"La capacidad debe ser mayor a 0\"}}"))),
            @ApiResponse(responseCode = "404", description = "El lugar especificado no existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Lugar no encontrado con el ID: 5\"}"))),
            @ApiResponse(responseCode = "409", description = "Nombre de sector duplicado en el mismo lugar",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":409,\"message\":\"Ya existe un sector llamado 'Campo' en este lugar\"}"))),
            @ApiResponse(responseCode = "422", description = "Inconsistencia de negocio en las reglas del sector (ej. capacidad no coincide con filas x asientos)",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":422,\"message\":\"La capacidad total declarada (50) no coincide con el producto de filas (10) x asientos por fila (10) = 100\"}")))
    })
    public ResponseEntity<SectorResponseDTO> createSector(
            @Parameter(description = "ID del lugar contenedor", example = "1") @PathVariable Long venueId,
            @Valid @RequestBody SectorRequestDTO request) {
        SectorResponseDTO response = venueService.createSector(venueId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{venueId}/sectors")
    @Operation(summary = "Listar sectores de un lugar", description = "Retorna todos los sectores (numerados y no numerados) asociados al `venueId` dado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sectores devueltos exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SectorResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "El lugar especificado no existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Lugar no encontrado con el ID: 8\"}")))
    })
    public ResponseEntity<List<SectorResponseDTO>> getSectorsByVenue(@PathVariable Long venueId) {
        return ResponseEntity.ok(venueService.getSectorsByVenue(venueId));
    }
}