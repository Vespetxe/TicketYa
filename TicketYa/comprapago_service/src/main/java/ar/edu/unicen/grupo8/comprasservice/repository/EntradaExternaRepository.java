package ar.edu.unicen.grupo8.comprasservice.repository;
import ar.edu.unicen.grupo8.comprasservice.entity.EntradaExterna;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface EntradaExternaRepository extends JpaRepository<EntradaExterna,Long> {
    Optional<EntradaExterna> findByUuid(String uuid);
}
