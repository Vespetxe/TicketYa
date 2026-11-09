package example.microservicio_inventario_reservas;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EntornoPruebasSeguroTest {
    @Test void bloqueaDatasourceDeDesarrolloAntesDeHibernate() {
        try (var context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("spring.datasource.url", "jdbc:mysql://localhost:3306/db_inventario_reservas")));
            assertThrows(IllegalStateException.class, () -> new EntornoPruebasSeguro().initialize(context));
        }
    }
    @Test void admiteSoloLaBaseEnMemoriaDeLaSuite() {
        try (var context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("spring.datasource.url", "jdbc:h2:mem:ticketya_inventario_test;MODE=MySQL")));
            assertDoesNotThrow(() -> new EntornoPruebasSeguro().initialize(context));
        }
    }
}
