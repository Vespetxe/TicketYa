package ar.edu.unicen.grupo8.comprasservice.client;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.util.function.UnaryOperator;

@Component
public class AlmacenSimuladoJpa implements AlmacenSimulado {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public AlmacenSimuladoJpa(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T actualizar(String clave, Class<T> tipo, UnaryOperator<T> accion) {
        jdbc.update("INSERT INTO operaciones_simuladas (clave, datos) VALUES (?, NULL) ON DUPLICATE KEY UPDATE clave=clave", clave);
        String json = jdbc.queryForObject("SELECT datos FROM operaciones_simuladas WHERE clave=? FOR UPDATE", String.class, clave);
        T anterior = json == null ? null : mapper.readValue(json, tipo);
        T nuevo = accion.apply(anterior);
        jdbc.update("UPDATE operaciones_simuladas SET datos=? WHERE clave=?", nuevo == null ? null : mapper.writeValueAsString(nuevo), clave);
        return nuevo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void limpiar(String prefijo) {
        jdbc.update("DELETE FROM operaciones_simuladas WHERE clave LIKE ?", prefijo + "%");
    }
}
