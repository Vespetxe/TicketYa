package ar.edu.tudai.ticketya.servicio_entradas.integration;
public class OrderNotConfirmedException extends RuntimeException {
    public OrderNotConfirmedException(String orderId) { super("Order is not confirmed: " + orderId); }
}
