package ar.edu.unicen.grupo8.comprasservice.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AplicacionConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
