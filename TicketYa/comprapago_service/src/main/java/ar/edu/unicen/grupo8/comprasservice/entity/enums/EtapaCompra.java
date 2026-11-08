package ar.edu.unicen.grupo8.comprasservice.entity.enums;

/** Punto durable desde el que continúa el mismo pago. */
public enum EtapaCompra {
    PAGO, CONFIRMACION, EMISION, COMPENSACION, LIBERACION, FINALIZADA
}
