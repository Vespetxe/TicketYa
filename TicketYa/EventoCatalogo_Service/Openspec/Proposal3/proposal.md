# Proposal 03: Búsqueda Avanzada, Catálogo y Filtros de Eventos (`busqueda-catalogo-eventos`)

## 1. Proposal
* **Funcionalidades cubiertas:** Cobertura de la **Funcionalidad 9** del enunciado general del TPE TicketYa.
* **Descripción:** Provee endpoints públicos para la búsqueda de eventos dentro del catálogo activo (estados `PUBLICADO` o `AGOTADO`). Admite filtros combinados por tipo de evento, organizador, participante, ciudad (vía el lugar donde se realiza) y rango de fechas, ordenados por fecha, precio o relevancia.
* **Afectación de Contratos Externos:** **NO AFECTA.**
    * Son endpoints de consulta orientados a ser consumidos por los clientes finales a través del API Gateway. No impactan en las interfaces internas de los otros microservicios.

---