package ar.edu.tudai.ticketya.servicio_entradas.integration;
import java.util.List;
public record OrderSnapshot(String orderId, String clientId, String status, List<OrderItemSnapshot> items) {}
