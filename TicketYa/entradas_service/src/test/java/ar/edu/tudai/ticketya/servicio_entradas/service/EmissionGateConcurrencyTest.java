package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.integration.*;
import ar.edu.tudai.ticketya.servicio_entradas.repository.TicketRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class EmissionGateConcurrencyTest {
    @Autowired EmissionGateService gate;
    @Autowired TicketBatchWriter writer;
    @Autowired TicketRepository tickets;
    @Autowired PlatformTransactionManager manager;
    @Test void cancelacionEsperaEmisionEnCursoYBloqueaTodosLosReintentosPosteriores() throws Exception {
        String event=UUID.randomUUID().toString();var entered=new CountDownLatch(1);var finish=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var emission=pool.submit(()->new TransactionTemplate(manager).execute(tx->{
                writer.create(new OrderSnapshot("before-"+event,"client","CONFIRMADA",List.of(new OrderItemSnapshot(event,"sector",null,1))));
                entered.countDown();try{if(!finish.await(10,TimeUnit.SECONDS))throw new IllegalStateException("Timeout");}catch(InterruptedException ex){throw new IllegalStateException(ex);}return null;
            }));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            var cancelling=new CountDownLatch(1);
            var cancellation=pool.submit(()->{cancelling.countDown();gate.cancel(event);});
            assertTrue(cancelling.await(5,TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,()->cancellation.get(150,TimeUnit.MILLISECONDS));
            finish.countDown();emission.get(10,TimeUnit.SECONDS);cancellation.get(10,TimeUnit.SECONDS);
            gate.cancel(event);
            for(int n=0;n<3;n++){
                String id="after-"+n+event;
                assertThrows(OrderNotConfirmedException.class,()->writer.create(new OrderSnapshot(id,"client","CONFIRMADA",List.of(new OrderItemSnapshot(event,"sector",null,1)))));
                assertTrue(tickets.findAllByBatch_OrderIdOrderByTicketId(id).isEmpty());
            }
        }finally{finish.countDown();pool.shutdownNow();}
    }
}
