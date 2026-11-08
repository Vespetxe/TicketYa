package grupo22.eventocatalogo_service.repository;

import grupo22.eventocatalogo_service.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long>, JpaSpecificationExecutor<Venue> {
    // Método de búsqueda por ciudad o parte de la dirección
    List<Venue> findByAddressContainingIgnoreCase(String city);
}