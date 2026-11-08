package grupo22.eventocatalogo_service.repository;

import grupo22.eventocatalogo_service.domain.EventSectorPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSectorPriceRepository extends JpaRepository<EventSectorPrice, Long> {
    List<EventSectorPrice> findByEventEventId(Long eventId);
    Optional<EventSectorPrice> findByEventEventIdAndSectorId(Long eventId, Long sectorId);
}