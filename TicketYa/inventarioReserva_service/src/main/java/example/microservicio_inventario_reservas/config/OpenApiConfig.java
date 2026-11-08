package example.microservicio_inventario_reservas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Inventario y Reservas - TicketYa")
                        .version("1.0.0")
                        .description("Microservicio encargado de la gestión exclusiva del stock, aforos, asientos numerados, " +
                                "creación de reservas temporales con TTL y prevención de la doble venta.")
                        .contact(new Contact().name("Grupo TicketYa - TUDAI")));
    }
}
