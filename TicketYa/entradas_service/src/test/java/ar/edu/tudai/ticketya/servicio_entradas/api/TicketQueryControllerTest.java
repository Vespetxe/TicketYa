package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.service.TicketQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class TicketQueryControllerTest {
    private TicketQueryService queries;
    private MockMvc mvc;
    @BeforeEach void setUp() {
        queries = mock(TicketQueryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new TicketQueryController(queries))
                .setControllerAdvice(new TicketApiExceptionHandler()).build();
    }
    @Test void queriesByOrderId() throws Exception {
        when(queries.byOrderId("ord-1")).thenReturn(List.of(
                new TicketResponse("t-1", "qr-1", "ord-1", "evt-1", "cli-1",
                        "platea", "seat-7", "EMITIDA")));
        mvc.perform(get("/tickets").queryParam("orderId", "ord-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("ord-1"))
                .andExpect(jsonPath("$[0].seatId").value("seat-7"));
    }
    @Test void queryWithoutTicketsReturnsEmptyCollection() throws Exception {
        when(queries.byClientId("cli-empty")).thenReturn(List.of());
        mvc.perform(get("/tickets").queryParam("clientId", "cli-empty"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
    @Test void requiresExactlyOneFilter() throws Exception {
        mvc.perform(get("/tickets")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TICKET_QUERY"));
        mvc.perform(get("/tickets").queryParam("orderId", "ord-1").queryParam("clientId", "cli-1"))
                .andExpect(status().isBadRequest());
    }
}
