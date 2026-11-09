package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.domain.*;
import ar.edu.tudai.ticketya.servicio_entradas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TicketAnnulmentService {
    private final TicketBatchRepository batches;private final TicketRepository tickets;
    public TicketAnnulmentService(TicketBatchRepository b,TicketRepository t){batches=b;tickets=t;}
    @Transactional
    public void annul(String id){
        TicketBatch batch=batches.lockByOrderId(id).orElseGet(()->batches.saveAndFlush(new TicketBatch(id)));
        batch.cancel();
        var values=tickets.findAllByBatch_OrderIdOrderByTicketId(id);
        values.forEach(t->{if(t.getStatus()!=TicketStatus.REINTEGRADA)t.setStatus(TicketStatus.ANULADA);});
        tickets.saveAllAndFlush(values);
    }
}
