package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketQueryService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
@RestController
public class TicketQueryController {
    private final TicketQueryService queries;
    public TicketQueryController(TicketQueryService queries) { this.queries = queries; }
    @GetMapping("/tickets")
    public List<TicketResponse> query(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String clientId) {
        boolean hasOrderId = StringUtils.hasText(orderId);
        boolean hasClientId = StringUtils.hasText(clientId);
        if (hasOrderId == hasClientId) throw new InvalidTicketQueryException();
        return hasOrderId ? queries.byOrderId(orderId.trim()) : queries.byClientId(clientId.trim());
    }
}
