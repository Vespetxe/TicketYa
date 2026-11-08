# Proposal 01: Gestión de Lugares, Sectores y Asientos/Aforos (`crud-lugares-sectores`)

## 1. Proposal
* **Funcionalidades cubiertas:** Cobertura de la **Funcionalidad 2** del enunciado general del TPE TicketYa.
* **Descripción:** Este cambio implementa el dominio de infraestructura física y virtual donde se realizan los eventos. Permite la administración (alta, modificación, consulta y baja/deshabilitación) de los lugares (estadios, teatros, auditorios), sus sectores correspondientes y la definición de capacidad/asientos asociados.
* **Afectación de Contratos Externos:** **SÍ AFECTA.**
    * Expone la estructura de lugares y sectores (`sectorId`, tipo de sector, capacidad o distribución de filas/asientos) que será consumida de manera síncrona por el microservicio `inventario-reservas` al momento de inicializar el inventario de cupos para un evento publicado.

---