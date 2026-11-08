package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketIssuanceResult;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketIssuanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
class TicketControllerTest {
    private TicketIssuanceService issuance;
    private MockMvc mvc;
    @BeforeEach void setUp() {
        issuance = mock(TicketIssuanceService.class);
        mvc = MockMvcBuilders.standaloneSetup(new TicketController(issuance))
                .setControllerAdvice(new TicketApiExceptionHandler()).build();
    }
    @Test void firstIssueReturnsCreatedAndTicketReferences() throws Exception {
        TicketResponse ticket = new TicketResponse("t-1", "qr-1", "ord-1", "evt-1",
                "cli-1", "campo", null, "EMITIDA");
        when(issuance.issue("ord-1")).thenReturn(new TicketIssuanceResult(List.of(ticket), true));
        mvc.perform(post("/internal/tickets/issue").contentType("application/json")
                        .content("{\"orderId\":\"ord-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tickets[0].orderId").value("ord-1"))
                .andExpect(jsonPath("$.tickets[0].seatId").doesNotExist())
                .andExpect(jsonPath("$.created").value(true));
    }
    @Test void retryReturnsOk() throws Exception {
        when(issuance.issue("ord-1")).thenReturn(new TicketIssuanceResult(List.of(), false));
        mvc.perform(post("/internal/tickets/issue").contentType("application/json")
                        .content("{\"orderId\":\"ord-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(false));
    }
    @Test void missingOrderReturnsNotFound() throws Exception {
        when(issuance.issue("missing")).thenThrow(new ar.edu.tudai.ticketya.servicio_entradas.integration.OrderNotFoundException("missing"));
        mvc.perform(post("/internal/tickets/issue").contentType("application/json")
                        .content("{\"orderId\":\"missing\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }
}
