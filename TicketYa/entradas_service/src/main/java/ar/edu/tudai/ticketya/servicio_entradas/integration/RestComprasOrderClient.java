package ar.edu.tudai.ticketya.servicio_entradas.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
@Component
public class RestComprasOrderClient implements OrderClient {
    private final RestClient restClient;
    public RestComprasOrderClient(RestClient.Builder builder,
            @Value("${ticketya.compras.base-url:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }
    @Override
    public OrderSnapshot getOrder(String orderId) {
        final OrderSnapshot order;
        try {
            order = restClient.get().uri("/internal/orders/{orderId}", orderId)
                    .retrieve().body(OrderSnapshot.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) throw new OrderNotFoundException(orderId);
            throw new OrdersUnavailableException("Orders service returned HTTP "
                    + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw new OrdersUnavailableException("Orders service is unavailable");
        }
        if (order == null || !orderId.equals(order.orderId()) || order.clientId() == null
                || order.items() == null || order.items().isEmpty()) {
            throw new OrdersUnavailableException("Orders service returned an invalid order response");
        }
        if (!"CONFIRMADA".equalsIgnoreCase(order.status())) throw new OrderNotConfirmedException(orderId);
        return order;
    }
}
