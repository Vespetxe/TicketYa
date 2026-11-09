package example.microservicio_inventario_reservas.repositories;

import example.microservicio_inventario_reservas.entities.Reserva;
import example.microservicio_inventario_reservas.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("ReservaRepository")
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Reserva r where r.idReserva = :id")
    java.util.Optional<Reserva> buscarConBloqueo(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select r.idReserva from Reserva r where r.estado = example.microservicio_inventario_reservas.enums.EstadoReserva.PENDIENTE and r.fechaExpiracion < :ahora")
    List<Long> buscarIdsVencidos(@org.springframework.data.repository.query.Param("ahora") LocalDateTime ahora);

    List<Reserva> findByIdCliente(Long idCliente);

    List<Reserva> findByEstadoAndFechaExpiracionBefore(EstadoReserva estado, LocalDateTime fechaLimite);

    List<Reserva> findByIdEventoAndEstado(Long idEvento, EstadoReserva estado);
}
