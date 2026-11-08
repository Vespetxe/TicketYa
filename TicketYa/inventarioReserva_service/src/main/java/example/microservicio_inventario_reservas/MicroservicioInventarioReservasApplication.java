package example.microservicio_inventario_reservas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Habilita las tareas programadas con @Scheduled
public class MicroservicioInventarioReservasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicroservicioInventarioReservasApplication.class, args);
    }

}
