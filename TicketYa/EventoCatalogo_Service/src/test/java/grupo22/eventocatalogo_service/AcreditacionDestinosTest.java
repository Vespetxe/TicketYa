package grupo22.eventocatalogo_service;
import grupo22.eventocatalogo_service.controller.TestEnvironmentController;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AcreditacionDestinosTest {
    private MockEnvironment environment() {
        var env=new MockEnvironment().withProperty("TICKETYA_TEST_TOKEN","a".repeat(32))
            .withProperty("spring.datasource.url","jdbc:mysql://127.0.0.1:33479/ticketya_e1_eventos?useSSL=false");
        env.setProperty("ticketya.inventario.base-url","http://127.0.0.1:18082");
        env.setProperty("ticketya.entradas.base-url","http://127.0.0.1:18083");
        env.setProperty("ticketya.compras.base-url","http://127.0.0.1:18084");
        return env;
    }
    @Test void cadaDestinoNormalSeRechazaAntesDeConsultarLaBase() {
        for(String destination:new String[]{"inventario","entradas","compras"}) {
            var env=environment();env.setProperty("ticketya."+destination+".base-url","http://localhost:8082");
            var jdbc=mock(JdbcTemplate.class);
            var error=assertThrows(ResponseStatusException.class,()->new TestEnvironmentController(env,jdbc).verify("a".repeat(32)));
            assertEquals(412,error.getStatusCode().value());verifyNoInteractions(jdbc);
        }
    }
    @Test void acreditaTodosLosDestinosCorrectos() {
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("ticketya_e1_eventos");
        var result=new TestEnvironmentController(environment(),jdbc).verify("a".repeat(32));
        assertEquals(5,result.size());assertEquals("ticketya_e1_eventos",result.get("database"));
    }
    @Test void segundaRedSeAcreditaSinAdmitirDestinosDeLaPrimera() {
        var env=environment().withProperty("TICKETYA_TEST_PORT_OFFSET","10000")
            .withProperty("spring.datasource.url","jdbc:mysql://127.0.0.1:33489/ticketya_e1_eventos?useSSL=false");
        env.setProperty("ticketya.inventario.base-url","http://127.0.0.1:28082");
        env.setProperty("ticketya.entradas.base-url","http://127.0.0.1:28083");
        env.setProperty("ticketya.compras.base-url","http://127.0.0.1:28084");
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("ticketya_e1_eventos");
        new TestEnvironmentController(env,jdbc).verify("a".repeat(32));
        env.setProperty("ticketya.inventario.base-url","http://127.0.0.1:18082");
        assertThrows(ResponseStatusException.class,()->new TestEnvironmentController(env,jdbc).verify("a".repeat(32)));
        verify(jdbc,times(1)).queryForObject("SELECT DATABASE()",String.class);
    }
}
