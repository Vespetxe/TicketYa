package ar.edu.tudai.ticketya.servicio_entradas;
import ar.edu.tudai.ticketya.servicio_entradas.api.TestEnvironmentController;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class AcreditacionDestinosTest {
    private MockEnvironment environment() {
        var env=new MockEnvironment().withProperty("TICKETYA_TEST_TOKEN","a".repeat(32))
            .withProperty("spring.datasource.url","jdbc:mysql://127.0.0.1:33479/ticketya_e1_entradas?useSSL=false");
        env.setProperty("ticketya.compras.base-url","http://127.0.0.1:18084");
        return env;
    }
    @Test void cadaDestinoNormalSeRechazaAntesDeConsultarLaBase() {
        for(String destination:new String[]{"compras"}) {
            var env=environment();env.setProperty("ticketya."+destination+".base-url","http://localhost:8082");
            var jdbc=mock(JdbcTemplate.class);
            var error=assertThrows(ResponseStatusException.class,()->new TestEnvironmentController(env,jdbc).verify("a".repeat(32)));
            assertEquals(412,error.getStatusCode().value());verifyNoInteractions(jdbc);
        }
    }
    @Test void acreditaTodosLosDestinosCorrectos() {
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("ticketya_e1_entradas");
        var result=new TestEnvironmentController(environment(),jdbc).verify("a".repeat(32));
        assertEquals(3,result.size());assertEquals("ticketya_e1_entradas",result.get("database"));
    }
    @Test void segundaRedSeAcreditaSinAdmitirDestinosDeLaPrimera() {
        var env=environment().withProperty("TICKETYA_TEST_PORT_OFFSET","10000")
            .withProperty("spring.datasource.url","jdbc:mysql://127.0.0.1:33489/ticketya_e1_entradas?useSSL=false");
        env.setProperty("ticketya.compras.base-url","http://127.0.0.1:28084");
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject("SELECT DATABASE()",String.class)).thenReturn("ticketya_e1_entradas");
        new TestEnvironmentController(env,jdbc).verify("a".repeat(32));
        env.setProperty("ticketya.compras.base-url","http://127.0.0.1:18084");
        assertThrows(ResponseStatusException.class,()->new TestEnvironmentController(env,jdbc).verify("a".repeat(32)));
        verify(jdbc,times(1)).queryForObject("SELECT DATABASE()",String.class);
    }
}
