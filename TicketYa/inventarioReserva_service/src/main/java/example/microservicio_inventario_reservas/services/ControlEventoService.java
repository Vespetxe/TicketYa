package example.microservicio_inventario_reservas.services;
import example.microservicio_inventario_reservas.repositories.ControlEventoRepository;
import example.microservicio_inventario_reservas.entities.ControlEvento;
import example.microservicio_inventario_reservas.enums.EstadoInventario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional
public class ControlEventoService {
    private final ControlEventoRepository repository;
    public ControlEventoService(ControlEventoRepository r){repository=r;}
    private ControlEvento lock(Long id){repository.asegurar(id);return repository.bloquear(id).orElseThrow();}
    public void inicializar(Long id){var estado=lock(id).getEstado();if(estado!=EstadoInventario.PREPARADO && estado!=EstadoInventario.HABILITADO)throw new IllegalStateException("Evento suspendido o cancelado");}
    public void publicar(Long id){
        var c=lock(id);
        if(c.getEstado()!=EstadoInventario.PREPARADO && c.getEstado()!=EstadoInventario.HABILITADO)
            throw new IllegalStateException("Una publicacion tardia no puede reabrir un evento bloqueado");
        c.setEstado(EstadoInventario.HABILITADO);
    }
    public void cancelar(Long id){lock(id).setEstado(EstadoInventario.CANCELADO);}
    public void suspender(Long id){var c=lock(id);if(c.getEstado()!=EstadoInventario.CANCELADO)c.setEstado(EstadoInventario.DESHABILITADO);}
    public void habilitar(Long id){var c=lock(id);if(c.getEstado()==EstadoInventario.CANCELADO)throw new IllegalStateException("Evento cancelado");c.setEstado(EstadoInventario.HABILITADO);}
}
