package grupo22.eventocatalogo_service.DTO.request;

public class ParticipantRequestDTO {

    private String name;

    private String role;

    public ParticipantRequestDTO() {}

    public ParticipantRequestDTO(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}