package ar.edu.unicen.grupo8.comprasservice;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Se ejecuta antes de Hibernate: una variable de entorno no puede redirigir tests a desarrollo. */
public class EntornoPruebasSeguro implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        String url = contexto.getEnvironment().getProperty("spring.datasource.url", "");
        if (!url.matches("jdbc:mysql://127\\.0\\.0\\.1:33079/ticketya_compras_test(?:\\?.*)?")) {
            throw new IllegalStateException("Tests bloqueados: se requiere MySQL aislado en 127.0.0.1:33079/ticketya_compras_test");
        }
    }
}
