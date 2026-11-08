package grupo22.eventocatalogo_service.service;

import grupo22.eventocatalogo_service.domain.Seat;
import grupo22.eventocatalogo_service.domain.Sector;
import grupo22.eventocatalogo_service.domain.SectorType;
import grupo22.eventocatalogo_service.domain.Venue;
import grupo22.eventocatalogo_service.DTO.request.SectorRequestDTO;
import grupo22.eventocatalogo_service.DTO.request.VenueRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.SectorResponseDTO;
import grupo22.eventocatalogo_service.DTO.response.VenueResponseDTO;
import grupo22.eventocatalogo_service.exception.BusinessRuleException;
import grupo22.eventocatalogo_service.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import grupo22.eventocatalogo_service.repository.SectorRepository;
import grupo22.eventocatalogo_service.repository.VenueRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VenueService {

    private final VenueRepository venueRepository;
    private final SectorRepository sectorRepository;

    public VenueService(VenueRepository venueRepository, SectorRepository sectorRepository) {
        this.venueRepository = venueRepository;
        this.sectorRepository = sectorRepository;
    }

    @Transactional
    public VenueResponseDTO createVenue(VenueRequestDTO request) {
        Venue venue = new Venue(
                request.getName(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude()
        );
        Venue saved = venueRepository.save(venue);
        return mapToVenueResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VenueResponseDTO> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(this::mapToVenueResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VenueResponseDTO getVenueById(Long venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado con el ID: " + venueId));
        return mapToVenueResponse(venue);
    }

    @Transactional
    public SectorResponseDTO createSector(Long venueId, SectorRequestDTO request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Lugar no encontrado con el ID: " + venueId));

        if (sectorRepository.existsByVenueVenueIdAndName(venueId, request.getName())) {
            throw new BusinessRuleException("Ya existe un sector con el nombre '" + request.getName() + "' en este lugar");
        }

        // Validación de reglas según el Tipo de Sector
        if (request.getType() == SectorType.NUMERADO) {
            validateNumeratedSectorRules(request);
        } else if (request.getType() == SectorType.NO_NUMERADO) {
            validateNonNumeratedSectorRules(request);
        }

        Sector sector = new Sector(venue, request.getName(), request.getType(), request.getCapacity());

        // Lógica de auto-generación de asientos para sectores numerados
        if (request.getType() == SectorType.NUMERADO) {
            if (request.getRows() == null || request.getSeatsPerRow() == null) {
                throw new BusinessRuleException("Para sectores numerados se requiere especificar filas y asientos por fila");
            }
            int calculatedCapacity = request.getRows() * request.getSeatsPerRow();
            if (calculatedCapacity != request.getCapacity()) {
                throw new BusinessRuleException("La capacidad declarada (" + request.getCapacity() +
                        ") no coincide con la grilla de filas x asientos (" + calculatedCapacity + ")");
            }

            for (int r = 1; r <= request.getRows(); r++) {
                String rowLabel = "Fila " + r;
                for (int s = 1; s <= request.getSeatsPerRow(); s++) {
                    Seat seat = new Seat(sector, rowLabel, s);
                    sector.addSeat(seat);
                }
            }
        }

        Sector savedSector = sectorRepository.save(sector);
        return new SectorResponseDTO(
                savedSector.getSectorId(),
                venueId,
                savedSector.getName(),
                savedSector.getType(),
                savedSector.getCapacity(),
                savedSector.getSeats().size()
        );
    }

    @Transactional(readOnly = true)
    public List<SectorResponseDTO> getSectorsByVenue(Long venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new ResourceNotFoundException("Lugar no encontrado con el ID: " + venueId);
        }

        return sectorRepository.findByVenueVenueId(venueId).stream()
                .map(sector -> new SectorResponseDTO(
                        sector.getSectorId(),
                        venueId,
                        sector.getName(),
                        sector.getType(),
                        sector.getCapacity(),
                        sector.getSeats().size()
                ))
                .collect(Collectors.toList());
    }

    private VenueResponseDTO mapToVenueResponse(Venue venue) {
        return new VenueResponseDTO(
                venue.getVenueId(),
                venue.getName(),
                venue.getAddress(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getCreatedAt()
        );
    }

    private void validateNumeratedSectorRules(SectorRequestDTO request) {
        if (request.getRows() == null || request.getSeatsPerRow() == null) {
            throw new BusinessRuleException("Para un sector NUMERADO se debe especificar 'rows' y 'seatsPerRow'");
        }
        int expectedCapacity = request.getRows() * request.getSeatsPerRow();
        if (!request.getCapacity().equals(expectedCapacity)) {
            throw new BusinessRuleException(String.format(
                    "La capacidad total declarada (%d) no coincide con el producto de filas (%d) x asientos por fila (%d) = %d",
                    request.getCapacity(), request.getRows(), request.getSeatsPerRow(), expectedCapacity
            ));
        }
    }

    private void validateNonNumeratedSectorRules(SectorRequestDTO request) {
        if (request.getRows() != null || request.getSeatsPerRow() != null) {
            throw new BusinessRuleException("Un sector NO_NUMERADO no debe definir filas ni asientos por fila");
        }
    }
}