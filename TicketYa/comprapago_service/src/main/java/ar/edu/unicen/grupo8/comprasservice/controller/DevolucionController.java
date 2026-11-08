package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/devoluciones")
@Tag(name="Devoluciones", description="Solicitudes del cliente. Integracion futura: autenticar titular y autorizar la consulta.")
@ApiResponses({@ApiResponse(responseCode="400", description="Datos invalidos"),
        @ApiResponse(responseCode="404", description="Recurso inexistente"),
        @ApiResponse(responseCode="409", description="Estado o solicitud incompatible")})
public class DevolucionController {
    private final DevolucionService devoluciones;
    public DevolucionController(DevolucionService devoluciones) { this.devoluciones = devoluciones; }
    @PostMapping
    @Operation(summary="Solicitar devolucion de entradas", description="Anticipacion minima 72 horas UTC. No genera reintegros automaticamente.")
    @ApiResponse(responseCode="201", description="Solicitud pendiente registrada")
    public ResponseEntity<DevolucionDTO> solicitar(@Valid @RequestBody SolicitarDevolucionDTO dto) {
        var d = devoluciones.solicitar(dto);
        return ResponseEntity.created(java.net.URI.create("/devoluciones/" + d.id())).body(d);
    }
    @GetMapping("/{id}")
    @Operation(summary="Consultar una devolucion")
    public DevolucionDTO obtener(@PathVariable Long id) { return devoluciones.obtener(id); }
}
