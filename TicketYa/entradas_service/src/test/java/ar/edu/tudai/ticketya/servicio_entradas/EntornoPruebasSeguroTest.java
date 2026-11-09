package ar.edu.tudai.ticketya.servicio_entradas;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class EntornoPruebasSeguroTest {
    @Test void rechazaDatasourceExternoAntesDeHibernate(){
        try(var c=new GenericApplicationContext()){
            c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",Map.of("spring.datasource.url","jdbc:mysql://localhost:3306/ticketya_entradas")));
            assertThrows(IllegalStateException.class,()->new EntornoPruebasSeguro().initialize(c));
        }
    }
    @Test void permiteSoloMemoriaAislada(){
        try(var c=new GenericApplicationContext()){
            c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",Map.of("spring.datasource.url","jdbc:h2:mem:ticketya;MODE=MySQL")));
            assertDoesNotThrow(()->new EntornoPruebasSeguro().initialize(c));
        }
    }
}
