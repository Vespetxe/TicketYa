package grupo22.eventocatalogo_service.controller;

import grupo22.eventocatalogo_service.DTO.request.EventSearchFilterDTO;
import grupo22.eventocatalogo_service.DTO.request.EventSortOption;
import grupo22.eventocatalogo_service.DTO.response.EventSearchResultDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import grupo22.eventocatalogo_service.service.EventSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events/search")
@Tag(name = "Búsqueda y Catálogo", description = "Endpoints públicos para la búsqueda avanzada, filtrado dinámico y ordenamiento de eventos en el catálogo activo")
public class EventSearchController {

    private final EventSearchService eventSearchService;

    public EventSearchController(EventSearchService eventSearchService) {
        this.eventSearchService = eventSearchService;
    }

    @GetMapping
    @Operation(
            summary = "Búsqueda avanzada de eventos en el catálogo público",
            description = "Permite filtrar eventos activos (PUBLICADO o AGOTADO) aplicando múltiples criterios opcionales (tipo, organizador, artista/participante, ciudad y rango de fechas) y ordenando los resultados por fecha o precio."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda ejecutada exitosamente. Retorna la lista de eventos que cumplen los criterios junto con su precio mínimo por sector.",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = EventSearchResultDTO.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros de consulta inválidos o rango de fechas incoherente (ej. `fromDate` posterior a `toDate`).",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"timestamp\":\"2026-03-30T10:00:00Z\",\"status\":400,\"message\":\"La fecha desde ('fromDate') no puede ser posterior a la fecha hasta ('toDate')\"}"))
            )
    })
    public ResponseEntity<List<EventSearchResultDTO>> searchEvents(
                                                                   @Parameter(in = ParameterIn.QUERY, name = "type", description = "Filtrar por tipo de evento (ej. Recital, Teatro, Deporte, Conferencia)", example = "Recital")
                                                                   @RequestParam(required = false) String type,

                                                                   @Parameter(in = ParameterIn.QUERY, name = "organizerId", description = "Filtrar por ID único del organizador", example = "50")
                                                                       @RequestParam(required = false) Long organizerId,

                                                                   @Parameter(in = ParameterIn.QUERY, name = "participantId", description = "Filtrar por ID del artista o participante asociado", example = "2")
                                                                       @RequestParam(required = false) Long participantId,

                                                                   @Parameter(in = ParameterIn.QUERY, name = "city", description = "Filtrar por ciudad o coincidencia parcial de la dirección del lugar", example = "Tandil")
                                                                       @RequestParam(required = false) String city,

                                                                   @Parameter(in = ParameterIn.QUERY, name = "fromDate", description = "Fecha y hora inicial del rango de búsqueda (ISO 8601)", example = "2026-05-01T00:00:00-03:00")
                                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,

                                                                   @Parameter(in = ParameterIn.QUERY, name = "toDate", description = "Fecha y hora final del rango de búsqueda (ISO 8601)", example = "2026-05-31T23:59:59-03:00")
                                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,

                                                                   @Parameter(in = ParameterIn.QUERY, name = "sort", description = "Criterio de ordenamiento de los resultados", schema = @Schema(implementation = EventSortOption.class, defaultValue = "DATE_ASC"), example = "PRICE_ASC")
                                                                       @RequestParam(required = false, defaultValue = "DATE_ASC") EventSortOption sort) {

        EventSearchFilterDTO filter = new EventSearchFilterDTO(type, organizerId, participantId, city, fromDate, toDate);
        List<EventSearchResultDTO> results = eventSearchService.searchEvents(filter, sort);

        return ResponseEntity.ok(results);
    }
}
