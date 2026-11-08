package grupo22.eventocatalogo_service.repository;

import grupo22.eventocatalogo_service.domain.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findBySectorSectorId(Long sectorId);
}