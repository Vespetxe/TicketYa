package ar.edu.tudai.ticketya.servicio_entradas.integration;
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderId) { super("Order not found: " + orderId); }
}
