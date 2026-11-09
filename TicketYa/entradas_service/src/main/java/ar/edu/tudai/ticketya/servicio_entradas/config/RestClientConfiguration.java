package ar.edu.tudai.ticketya.servicio_entradas.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
@Configuration
public class RestClientConfiguration {
    @Bean
    RestClient.Builder restClientBuilder() {
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory);
    }
}
