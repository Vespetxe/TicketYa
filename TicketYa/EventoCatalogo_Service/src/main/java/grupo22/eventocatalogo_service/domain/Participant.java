package grupo22.eventocatalogo_service.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "participant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @NotBlank(message = "El nombre del participante/artista no puede estar vacío")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 50, message = "El rol/categoría no puede superar los 50 caracteres")
    @Column(name = "role", length = 50)
    private String role; // ej: "Banda", "Cantante Solista", "Disertante", "Equipo"

    public Participant() {}

    public Participant(String name, String role) {
        this.name = name;
        this.role = role;
    }

    // Getters y Setters
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
