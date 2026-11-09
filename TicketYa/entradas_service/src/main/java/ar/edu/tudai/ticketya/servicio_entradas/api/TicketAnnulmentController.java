package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketAnnulmentService;
import org.springframework.web.bind.annotation.*;
@RestController
public class TicketAnnulmentController {
    private final TicketAnnulmentService service;
    public TicketAnnulmentController(TicketAnnulmentService s){service=s;}
    @PostMapping("/internal/tickets/orders/{id}/annul")
    public void annul(@PathVariable String id){
        try{service.annul(id);}catch(org.springframework.dao.DataIntegrityViolationException race){service.annul(id);}
    }
}
