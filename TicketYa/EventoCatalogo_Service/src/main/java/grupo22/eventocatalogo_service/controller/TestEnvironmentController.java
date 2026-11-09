package grupo22.eventocatalogo_service.controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestController @Profile("pruebas-integrales")
public class TestEnvironmentController {
 private final Environment env;private final JdbcTemplate jdbc;
 public TestEnvironmentController(Environment e,JdbcTemplate j){env=e;jdbc=j;}
 @GetMapping("/internal/test-environment")
 public Map<String,String> verify(@RequestHeader("X-TicketYa-Test-Token") String token){
  String expected=env.getProperty("TICKETYA_TEST_TOKEN","");
  if(expected.length()<32 || !java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),expected.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  int offset=Integer.parseInt(env.getProperty("TICKETYA_TEST_PORT_OFFSET","0"));
  if(offset!=0 && offset!=10000)throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Red de prueba no permitida");
  var result=new java.util.HashMap<String,String>();
  String inventarioUrl=env.getProperty("ticketya.inventario.base-url", "");
  if(!inventarioUrl.equals("http://127.0.0.1:"+(18082+offset)))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Destino REST no aislado: inventario");
  result.put("inventario",inventarioUrl);
  String entradasUrl=env.getProperty("ticketya.entradas.base-url", "");
  if(!entradasUrl.equals("http://127.0.0.1:"+(18083+offset)))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Destino REST no aislado: entradas");
  result.put("entradas",entradasUrl);
  String comprasUrl=env.getProperty("ticketya.compras.base-url", "");
  if(!comprasUrl.equals("http://127.0.0.1:"+(18084+offset)))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Destino REST no aislado: compras");
  result.put("compras",comprasUrl);
  String database=jdbc.queryForObject("SELECT DATABASE()",String.class);
  String url=env.getProperty("spring.datasource.url","");
  if(!"ticketya_e1_eventos".equals(database) || !url.startsWith("jdbc:mysql://127.0.0.1:"+(33479+offset/1000)+"/ticketya_e1_eventos?"))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Base no aislada");
  result.put("service","eventos");result.put("database",database);return result;
 }
}
