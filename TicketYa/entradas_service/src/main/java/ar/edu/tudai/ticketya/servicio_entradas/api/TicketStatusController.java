package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketStatusService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
public class TicketStatusController {
    private final TicketStatusService statusService;
    public TicketStatusController(TicketStatusService statusService) { this.statusService = statusService; }
    @PatchMapping("/internal/tickets/{ticketId}/status")
    public TicketResponse updateOne(@PathVariable String ticketId,
            @Valid @RequestBody TicketStatusUpdateRequest request) {
        return statusService.updateOne(ticketId, request.orderId(), request.status());
    }
    @PatchMapping("/internal/tickets/status")
    public List<TicketResponse> updateForEvent(@Valid @RequestBody EventTicketsStatusUpdateRequest request) {
        return statusService.updateForOrderEvent(request.orderId(), request.eventId(), request.status());
    }
}
