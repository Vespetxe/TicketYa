package grupo22.eventocatalogo_service.repository;

import grupo22.eventocatalogo_service.domain.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Long> {
    List<Sector> findByVenueVenueId(Long venueId);
    boolean existsByVenueVenueIdAndName(Long venueId, String name);
}