package ar.edu.tudai.ticketya.servicio_entradas.domain;
import jakarta.persistence.*;
@Entity
@Table(name = "tickets", uniqueConstraints = @UniqueConstraint(name = "uk_ticket_qr_payload", columnNames = "qr_payload"))
public class Ticket {
    @Version private Long version;
    @Id @Column(name = "ticket_id", nullable = false, length = 36)
    private String ticketId;
    @Column(name = "qr_payload", nullable = false, length = 36)
    private String qrPayload;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private TicketBatch batch;
    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;
    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;
    @Column(name = "sector_id", nullable = false, length = 64)
    private String sectorId;
    @Column(name = "seat_id", length = 64)
    private String seatId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TicketStatus status;
    protected Ticket() {}
    public Ticket(String ticketId, String qrPayload, TicketBatch batch, String eventId,
                  String clientId, String sectorId, String seatId) {
        this.ticketId = ticketId; this.qrPayload = qrPayload; this.batch = batch;
        this.eventId = eventId; this.clientId = clientId; this.sectorId = sectorId;
        this.seatId = seatId; this.status = TicketStatus.EMITIDA;
    }
    public String getTicketId() { return ticketId; }
    public String getQrPayload() { return qrPayload; }
    public TicketBatch getBatch() { return batch; }
    public String getOrderId() { return batch.getOrderId(); }
    public String getEventId() { return eventId; }
    public String getClientId() { return clientId; }
    public String getSectorId() { return sectorId; }
    public String getSeatId() { return seatId; }
    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
}
