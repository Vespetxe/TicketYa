package ar.edu.tudai.ticketya.servicio_entradas.service;
import ar.edu.tudai.ticketya.servicio_entradas.repository.EmissionEventGateRepository;
import ar.edu.tudai.ticketya.servicio_entradas.integration.OrderNotConfirmedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class EmissionGateService {
    private final EmissionEventGateRepository repository;
    public EmissionGateService(EmissionEventGateRepository r){repository=r;}
    public void allow(String id){repository.ensure(id);if(repository.lock(id).orElseThrow().isCancelled())throw new OrderNotConfirmedException(id);}
    public void cancel(String id){repository.ensure(id);repository.lock(id).orElseThrow().cancel();}
}
