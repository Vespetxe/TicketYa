# Proposal 02: Ciclo de Vida y Configuración de Eventos (`gestion-eventos`)

## 1. Proposal
* **Funcionalidades cubiertas:** Cobertura de las **Funcionalidades 1 y 3** del enunciado general del TPE TicketYa.
* **Descripción:** Administra la creación de eventos en borrador, la asignación de organizadores y participantes (artistas, equipos, disertantes), la configuración de la matriz de precios por sector y el control estricto del estado del evento (`borrador`, `publicado`, `agotado`, `cancelado`, `finalizado`).
* **Afectación de Contratos Externos:** **SÍ AFECTA.**
    * Expone los endpoints de consulta de eventos, vigencia y precios por sector que consume **`compras-pagos`** para congelar/validar importes.
    * Notifica las transiciones a estado `cancelado` para activar los flujos de reintegro en el servicio de compras y la anulación de entradas.

---