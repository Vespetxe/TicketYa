package grupo22.eventocatalogo_service.service;

import grupo22.eventocatalogo_service.DTO.request.ParticipantRequestDTO;
import grupo22.eventocatalogo_service.DTO.response.ParticipantResponseDTO;

import java.util.List;

public interface ParticipantService {
    ParticipantResponseDTO createParticipant(ParticipantRequestDTO request);
    List<ParticipantResponseDTO> getAllParticipants();
    ParticipantResponseDTO getParticipantById(Long id);
    ParticipantResponseDTO updateParticipant(Long id, ParticipantRequestDTO request);
    void deleteParticipant(Long id);
}