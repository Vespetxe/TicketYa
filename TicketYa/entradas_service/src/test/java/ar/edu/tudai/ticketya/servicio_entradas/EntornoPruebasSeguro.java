package ar.edu.tudai.ticketya.servicio_entradas;
import org.springframework.context.*;
public class EntornoPruebasSeguro implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext context){
        if(!context.getEnvironment().getProperty("spring.datasource.url","").matches("jdbc:h2:mem:ticketya(?:;.*)?"))
            throw new IllegalStateException("Tests bloqueados: solo se permite H2 en memoria de Entradas");
    }
}
