package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.service.EmissionGateService;
import org.springframework.web.bind.annotation.*;
@RestController
public class EventEmissionController {
    private final EmissionGateService service;
    public EventEmissionController(EmissionGateService s){service=s;}
    @PostMapping("/internal/events/{id}/cancel-emission") public void cancel(@PathVariable String id){service.cancel(id);}
}
