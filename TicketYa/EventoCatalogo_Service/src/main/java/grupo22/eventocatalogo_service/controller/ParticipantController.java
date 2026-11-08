package grupo22.eventocatalogo_service.controller;

import grupo22.eventocatalogo_service.DTO.request.ParticipantRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.ParticipantResponseDTO;
import grupo22.eventocatalogo_service.service.ParticipantService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/participants")
@Tag(name = "Participantes", description = "Endpoints para la gestión (CRUD) de artistas y participantes de eventos")
public class ParticipantController {

    private final ParticipantService participantService;

    public ParticipantController(ParticipantService participantService) {
        this.participantService = participantService;
    }

    @PostMapping
    @Operation(summary = "Crear participante", description = "Registra un nuevo participante o artista en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Participante creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParticipantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-11-08T14:00:00Z\",\"status\":400,\"errors\":{\"name\":\"El nombre no puede estar vacío\"}}")))
    })
    public ResponseEntity<ParticipantResponseDTO> createParticipant(@Valid @RequestBody ParticipantRequestDTO request) {
        ParticipantResponseDTO response = participantService.createParticipant(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar participantes", description = "Recupera la lista completa de todos los participantes registrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista devuelta exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = ParticipantResponseDTO.class))))
    })
    public ResponseEntity<List<ParticipantResponseDTO>> getAllParticipants() {
        return ResponseEntity.ok(participantService.getAllParticipants());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener participante por ID", description = "Busca el detalle de un participante según su ID único.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Participante encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParticipantResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Participante no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-11-08T14:00:00Z\",\"status\":404,\"message\":\"Participante no encontrado con el ID: 99\"}")))
    })
    public ResponseEntity<ParticipantResponseDTO> getParticipantById(
            @Parameter(description = "ID del participante a consultar", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(participantService.getParticipantById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar participante", description = "Modifica los datos de un participante existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Participante actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParticipantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Participante no encontrado")
    })
    public ResponseEntity<ParticipantResponseDTO> updateParticipant(
            @Parameter(description = "ID del participante a actualizar", example = "1") @PathVariable Long id,
            @Valid @RequestBody ParticipantRequestDTO request) {
        return ResponseEntity.ok(participantService.updateParticipant(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar participante", description = "Elimina permanentemente un participante del registro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Participante eliminado exitosamente (Sin contenido)"),
            @ApiResponse(responseCode = "404", description = "Participante no encontrado")
    })
    public ResponseEntity<Void> deleteParticipant(
            @Parameter(description = "ID del participante a eliminar", example = "1") @PathVariable Long id) {
        participantService.deleteParticipant(id);
        return ResponseEntity.noContent().build();
    }
}