package example.microservicio_inventario_reservas.repositories;

import example.microservicio_inventario_reservas.entities.LugarDevuelto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface LugarDevueltoRepository extends JpaRepository<LugarDevuelto,Long> {
    Optional<LugarDevuelto> findByEntradaId(String entradaId);
    List<LugarDevuelto> findByReservaIdReserva(Long reservaId);
}
