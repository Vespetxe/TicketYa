package ar.edu.unicen.grupo8.comprasservice.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ar.edu.unicen.grupo8.comprasservice.repository.EntradaExternaRepository;
import ar.edu.unicen.grupo8.comprasservice.entity.EntradaExterna;
@Service
public class EntradaIdentidadService {
    private final EntradaExternaRepository repository;
    public EntradaIdentidadService(EntradaExternaRepository r){repository=r;}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public Long registrar(String uuid){
        return repository.findByUuid(uuid).orElseGet(()->{var e=new EntradaExterna();e.setUuid(uuid);return repository.saveAndFlush(e);}).getId();
    }
    @Transactional(readOnly=true)
    public String uuid(Long id){return repository.findById(id).orElseThrow(()->new IllegalArgumentException("Entrada desconocida")).getUuid();}
}
