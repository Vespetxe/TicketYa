package ar.edu.unicen.grupo8.comprasservice.controller;

import ar.edu.unicen.grupo8.comprasservice.dto.*;
import ar.edu.unicen.grupo8.comprasservice.entity.enums.EstadoDevolucion;
import ar.edu.unicen.grupo8.comprasservice.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin")
@Tag(name="Administracion de devoluciones", description="Requiere autorizacion administrativa en la futura integracion. Esta entrega no implementa seguridad.")
@ApiResponses({@ApiResponse(responseCode="400", description="Datos invalidos"),
        @ApiResponse(responseCode="404", description="Recurso inexistente"),
        @ApiResponse(responseCode="409", description="Estado, clave o procesamiento incompatible"),
        @ApiResponse(responseCode="503", description="Operacion externa no confirmada. Consultar y reintentar con la misma clave")})
public class DevolucionAdminController {
    private final DevolucionService devoluciones;
    private final ReintegroService reintegros;
    public DevolucionAdminController(DevolucionService devoluciones, ReintegroService reintegros) {
        this.devoluciones = devoluciones; this.reintegros = reintegros;
    }
    @GetMapping("/devoluciones")
    @Operation(summary="Consultar devoluciones por estado")
    public List<DevolucionDTO> consultar(@RequestParam EstadoDevolucion estado) { return devoluciones.obtenerPorEstado(estado); }
    @PostMapping("/devoluciones/{id}/aprobar")
    @Operation(summary="Aprobar una solicitud", description="Motivo administrativo obligatorio. No procesa el reintegro.")
    public DevolucionDTO aprobar(@PathVariable Long id, @Valid @RequestBody AprobarDevolucionDTO dto) { return devoluciones.aprobar(id, dto); }
    @PostMapping("/devoluciones/{id}/rechazar")
    @Operation(summary="Rechazar una solicitud", description="Motivo administrativo obligatorio.")
    public DevolucionDTO rechazar(@PathVariable Long id, @Valid @RequestBody RechazarDevolucionDTO dto) { return devoluciones.rechazar(id, dto); }
    @PostMapping("/devoluciones/{id}/procesar")
    @Operation(summary="Procesar o recuperar una devolucion aprobada",
            description="Reintegra todo el importe historico autorizado. Lo parcial se expresa seleccionando menos entradas. Reintentar con la misma clave e importe.")
    public ResultadoDevolucionDTO procesar(@PathVariable Long id, @Valid @RequestBody ProcesarDevolucionDTO dto) { return reintegros.procesar(id, dto); }
    @GetMapping("/devoluciones/{id}/procesamiento")
    @Operation(summary="Consultar avance y reintegro de una devolucion")
    public ResultadoDevolucionDTO obtener(@PathVariable Long id) { return reintegros.obtener(id); }
    @GetMapping("/reintegros/{id}")
    @Operation(summary="Consultar un reintegro por ID")
    public ReintegroDTO obtenerReintegro(@PathVariable Long id) { return reintegros.obtenerReintegro(id); }
    @PostMapping("/eventos/{id}/cancelacion")
    @Operation(summary="Recibir o reintentar cancelacion de evento simulada",
            description="Notificacion administrativa autoritativa: marca el evento simulado cancelado, omite las 72 horas, reutiliza solicitudes vigentes y no reofrece lugares. Devuelve 202 si quedan compras pendientes.")
    @ApiResponses({@ApiResponse(responseCode="200", description="Cancelacion procesada"),
            @ApiResponse(responseCode="202", description="Cancelacion registrada con compras pendientes; repetir para recuperar")})
    public ResponseEntity<CancelacionEventoDTO> cancelar(@PathVariable Long id) {
        var r = reintegros.cancelarEvento(id);
        return ResponseEntity.status(r.comprasPendientes().isEmpty() ? HttpStatus.OK : HttpStatus.ACCEPTED).body(r);
    }
}
