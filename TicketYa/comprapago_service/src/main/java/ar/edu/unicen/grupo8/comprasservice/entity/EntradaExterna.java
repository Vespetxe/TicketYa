package ar.edu.unicen.grupo8.comprasservice.entity;
import jakarta.persistence.*;
@Entity
public class EntradaExterna {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=36) private String uuid;
    public Long getId(){return id;}
    public String getUuid(){return uuid;}
    public void setUuid(String uuid){this.uuid=uuid;}
}
