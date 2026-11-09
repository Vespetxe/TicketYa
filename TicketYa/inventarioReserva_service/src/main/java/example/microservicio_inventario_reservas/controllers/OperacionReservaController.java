package example.microservicio_inventario_reservas.controllers;

import example.microservicio_inventario_reservas.dtos.*;
import example.microservicio_inventario_reservas.services.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/internal/reservas")
public class OperacionReservaController {
    private final DevolucionInventarioService devoluciones;
    private final ReservaService reservas;
    public OperacionReservaController(DevolucionInventarioService d, ReservaService r) {devoluciones=d; reservas=r;}
    @PostMapping("/{id}/devoluciones")
    public List<String> devolver(@PathVariable Long id,@Valid @RequestBody DevolverLugaresRequest request) {return devoluciones.devolver(id,request);}
    @GetMapping("/{id}/devoluciones")
    public List<String> consultar(@PathVariable Long id) {return devoluciones.consultar(id);}
    @PostMapping("/{id}/compensar")
    public MensajeResponse compensar(@PathVariable Long id) {return reservas.compensarReserva(id);}
}
