package grupo22.eventocatalogo_service.controller;

import grupo22.eventocatalogo_service.DTO.response.SeatResponseDTO;
import grupo22.eventocatalogo_service.domain.Seat;
import grupo22.eventocatalogo_service.domain.Sector;
import grupo22.eventocatalogo_service.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import grupo22.eventocatalogo_service.repository.SectorRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/sectors")
@Tag(name = "Asientos de catalogo", description = "Consulta de grilla accesible por Gateway; no sustituye disponibilidad operativa")
public class SectorController {

    private final SectorRepository sectorRepository;

    public SectorController(SectorRepository sectorRepository) {
        this.sectorRepository = sectorRepository;
    }

    @GetMapping("/{sectorId}/seats")
    @Operation(
            summary = "Obtener lista de asientos de un sector numerado",
            description = "Devuelve seatId, fila y numero de la grilla del catalogo. Para reservar utilizar idAsiento de disponibilidad en Inventario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de asientos devuelta exitosamente",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = SeatResponseDTO.class)))),
            @ApiResponse(responseCode = "404", description = "Sector no encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":404,\"message\":\"Sector no encontrado con ID: 45\"}")))
    })

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public ResponseEntity<List<SeatResponseDTO>> getSeatsBySector(@Parameter(description = "ID del sector a consultar", example = "2")@PathVariable Long sectorId) {
        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector no encontrado con ID: " + sectorId));

        List<SeatResponseDTO> response = sector.getSeats().stream()
                .map(seat -> new SeatResponseDTO(seat.getSeatId(), seat.getRowLabel(), seat.getSeatNumber()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
