package example.microservicio_inventario_reservas;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class EntornoPruebasSeguro implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String url = context.getEnvironment().getProperty("spring.datasource.url", "");
        if (!url.matches("jdbc:h2:mem:ticketya_inventario_test(?:;.*)?")
                && !url.matches("jdbc:mysql://127\\.0\\.0\\.1:33079/ticketya_inventario_test(?:\\?.*)?"))
            throw new IllegalStateException("Tests bloqueados: se requiere la base aislada de pruebas");
    }
}
