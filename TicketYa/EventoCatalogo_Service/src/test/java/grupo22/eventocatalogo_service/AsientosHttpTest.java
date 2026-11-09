package grupo22.eventocatalogo_service;

import grupo22.eventocatalogo_service.DTO.request.*;
import grupo22.eventocatalogo_service.domain.SectorType;
import grupo22.eventocatalogo_service.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AsientosHttpTest {
    @Autowired VenueService venues;
    @LocalServerPort int port;
    @Test void consultaGrillaPorHttpConOpenInViewDesactivado() throws Exception {
        var v=new VenueRequestDTO();v.setName("Sala HTTP");v.setAddress("Calle");v.setCity("Tandil");v.setLatitude(BigDecimal.ONE);v.setLongitude(BigDecimal.ONE);
        var venue=venues.createVenue(v);
        var s=new SectorRequestDTO();s.setName("Platea");s.setType(SectorType.NUMERADO);s.setCapacity(2);s.setRows(1);s.setSeatsPerRow(2);
        var sector=venues.createSector(venue.getVenueId(),s);
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/sectors/"+sector.getSectorId()+"/seats")).GET().build();
        var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode(),response.body());
        assertEquals(2,response.body().split("seatId",-1).length-1);
        assertTrue(response.body().contains("rowLabel"));assertTrue(response.body().contains("seatNumber"));
        assertFalse(response.body().contains("venue"));
    }
}
