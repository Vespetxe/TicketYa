package ar.edu.tudai.ticketya.servicio_entradas.domain;
import jakarta.persistence.*;
@Entity @Table(name="emission_event_gate")
public class EmissionEventGate {
    @Id @Column(name="event_id",length=64) private String eventId;
    private boolean cancelled;
    public boolean isCancelled(){return cancelled;}
    public void cancel(){cancelled=true;}
}
