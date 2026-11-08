package ar.edu.unicen.grupo8.comprasservice.entity;

import jakarta.persistence.*;

/** Diario de los servicios simulados; no representa una relación con sus bases reales. */
@Entity
@Table(name = "operaciones_simuladas")
public class OperacionSimulada {
    @Id
    @Column(length = 180)
    private String clave;
    @Column(columnDefinition = "LONGTEXT")
    private String datos;
    protected OperacionSimulada() { }
}
