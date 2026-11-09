package ar.edu.ticketya.gateway;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@RestController @Profile("pruebas-integrales")
public class TestEnvironmentController {
 private final Environment env;
 public TestEnvironmentController(Environment e){env=e;}
 @GetMapping("/internal/test-environment") public Map<String,String> verify(@RequestHeader("X-TicketYa-Test-Token") String token){
  String expected=env.getProperty("TICKETYA_TEST_TOKEN","");
  if(expected.length()<32 || !java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),expected.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  int offset=Integer.parseInt(env.getProperty("TICKETYA_TEST_PORT_OFFSET","0"));
  if(offset!=0 && offset!=10000)throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Red de prueba no permitida");
  var result=new java.util.HashMap<String,String>();result.put("service","gateway");
  for(var entry:Map.of("eventos",18081,"inventario",18082,"entradas",18083,"compras",18084).entrySet()){
   String url=env.getProperty("ticketya."+entry.getKey()+".url","");
   if(!url.equals("http://127.0.0.1:"+(entry.getValue()+offset)))throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,"Destino no aislado");
   result.put(entry.getKey(),url);
  }
  return result;
 }
}
