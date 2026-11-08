package grupo22.eventocatalogo_service.DTO.response;

public class ParticipantResponseDTO {
    private Long participantId;
    private String name;
    private String role;

    public ParticipantResponseDTO() {}

    public ParticipantResponseDTO(Long participantId, String name, String role) {
        this.participantId = participantId;
        this.name = name;
        this.role = role;
    }

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
