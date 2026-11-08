package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketIssuanceResult;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketIssuanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping
public class TicketController {
    private final TicketIssuanceService issuance;
    public TicketController(TicketIssuanceService issuance) { this.issuance = issuance; }
    @PostMapping("/internal/tickets/issue")
    public ResponseEntity<TicketIssuanceResult> issue(@Valid @RequestBody IssueTicketsRequest request) {
        TicketIssuanceResult result = issuance.issue(request.orderId());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }
}
