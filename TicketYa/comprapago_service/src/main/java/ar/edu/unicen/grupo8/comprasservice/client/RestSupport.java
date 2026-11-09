package ar.edu.unicen.grupo8.comprasservice.client;
import org.springframework.web.client.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import ar.edu.unicen.grupo8.comprasservice.exception.OperacionExternaRechazadaException;
public final class RestSupport {
    private RestSupport(){}
    public static RestClient client(String url){
        var http=java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(2)).build();
        var f=new org.springframework.http.client.JdkClientHttpRequestFactory(http);
        f.setReadTimeout(java.time.Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(url).requestFactory(f).build();
    }
    public static void mutate(Runnable action){
        try {action.run();}
        catch(RestClientResponseException ex){
            if(ex.getStatusCode().is4xxClientError())throw new OperacionExternaRechazadaException("Operacion rechazada: HTTP "+ex.getStatusCode().value());
            throw ex;
        }
    }
}
