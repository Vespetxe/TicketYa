package ar.edu.tudai.ticketya.servicio_entradas.domain;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "ticket_batches", uniqueConstraints = @UniqueConstraint(name = "uk_ticket_batch_order_id", columnNames = "order_id"))
public class TicketBatch {
    private boolean cancelled;
    public boolean isCancelled(){return cancelled;}
    public void cancel(){cancelled=true;}
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;
    protected TicketBatch() {}
    public TicketBatch(String orderId) { this.orderId = orderId; this.issuedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public Instant getIssuedAt() { return issuedAt; }
}
