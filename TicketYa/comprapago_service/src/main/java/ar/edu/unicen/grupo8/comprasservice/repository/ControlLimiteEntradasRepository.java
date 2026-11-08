
package ar.edu.unicen.grupo8.comprasservice.repository;

import ar.edu.unicen.grupo8.comprasservice.entity.ControlLimiteEntradas;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ControlLimiteEntradasRepository
        extends JpaRepository<ControlLimiteEntradas, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM ControlLimiteEntradas c
            WHERE c.clienteId = :clienteId
              AND c.eventoId = :eventoId
            """)
    Optional<ControlLimiteEntradas> buscarConBloqueo(
            @Param("clienteId") Long clienteId,
            @Param("eventoId") Long eventoId
    );


    @Modifying
    @Transactional
    @Query(
            value = """
                INSERT INTO control_limite_entradas
                    (cliente_id, evento_id)
                VALUES (:clienteId, :eventoId)
                ON DUPLICATE KEY UPDATE
                    id = id
                """,
            nativeQuery = true
    )
    void asegurarRegistro(
            @Param("clienteId") Long clienteId,
            @Param("eventoId") Long eventoId
    );

}
