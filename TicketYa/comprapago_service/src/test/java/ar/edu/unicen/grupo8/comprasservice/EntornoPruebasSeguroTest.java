package ar.edu.unicen.grupo8.comprasservice;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EntornoPruebasSeguroTest {
    @Test void bloqueaDatasourceDeDesarrolloAntesDeInicializarHibernate() {
        try (var contexto = new GenericApplicationContext()) {
            contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba",
                    Map.of("spring.datasource.url", "jdbc:mysql://localhost:3306/ticketya_compras")));
            assertThrows(IllegalStateException.class, () -> new EntornoPruebasSeguro().initialize(contexto));
        }
    }
    @Test void permiteSoloElDatasourceAislado() {
        try (var contexto = new GenericApplicationContext()) {
            contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba",
                    Map.of("spring.datasource.url", "jdbc:mysql://127.0.0.1:33079/ticketya_compras_test")));
            assertDoesNotThrow(() -> new EntornoPruebasSeguro().initialize(contexto));
        }
    }
}
