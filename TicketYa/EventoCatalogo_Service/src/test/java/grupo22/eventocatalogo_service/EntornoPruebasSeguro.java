package grupo22.eventocatalogo_service;
import org.springframework.context.*;
public class EntornoPruebasSeguro implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext context) {
        if(!context.getEnvironment().getProperty("spring.datasource.url","").matches("jdbc:h2:mem:ticketya_eventos_test(?:;.*)?"))
            throw new IllegalStateException("Solo se permite la base aislada de Eventos");
    }
}
