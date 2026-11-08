package ar.edu.tudai.ticketya.servicio_entradas.api;
import ar.edu.tudai.ticketya.servicio_entradas.domain.Ticket;
public record TicketResponse(String ticketId, String qr, String orderId, String eventId,
        String clientId, String sectorId, String seatId, String status) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(ticket.getTicketId(), ticket.getQrPayload(), ticket.getOrderId(),
                ticket.getEventId(), ticket.getClientId(), ticket.getSectorId(), ticket.getSeatId(),
                ticket.getStatus().name());
    }
}
