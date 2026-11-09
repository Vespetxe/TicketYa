package ar.edu.unicen.grupo8.comprasservice.controller;
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
  String eventosUrl=env.getProperty("ticketya.eventos.base-url", "");
  if(!eventosUrl.equals("http://127.0.0.1:"+(18081+offset)))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Destino REST no aislado: eventos");
  result.put("eventos",eventosUrl);
  String inventarioUrl=env.getProperty("ticketya.inventario.base-url", "");
  if(!inventarioUrl.equals("http://127.0.0.1:"+(18082+offset)))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Destino REST no aislado: inventario");
  result.put("inventario",inventarioUrl);
  String entradasUrl=env.getProperty("ticketya.entradas.base-url", "");
  if(!entradasUrl.equals("http://127.0.0.1:"+(18083+offset)))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Destino REST no aislado: entradas");
  result.put("entradas",entradasUrl);
  String database=jdbc.queryForObject("SELECT DATABASE()",String.class);
  String url=env.getProperty("spring.datasource.url","");
  if(!"ticketya_e1_compras".equals(database) || !url.startsWith("jdbc:mysql://127.0.0.1:"+(33479+offset/1000)+"/ticketya_e1_compras?"))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Base no aislada");
  result.put("service","compras");result.put("database",database);return result;
 }
}
