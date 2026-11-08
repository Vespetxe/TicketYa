package example.microservicio_inventario_reservas.repositories;

import example.microservicio_inventario_reservas.entities.Reserva;
import example.microservicio_inventario_reservas.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("ReservaRepository")
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByIdCliente(Long idCliente);

    List<Reserva> findByEstadoAndFechaExpiracionBefore(EstadoReserva estado, LocalDateTime fechaLimite);

    List<Reserva> findByIdEventoAndEstado(Long idEvento, EstadoReserva estado);
}
