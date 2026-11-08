# Propuesta: Microservicio de Compras y Pagos

## ¿Por qué es necesario?

TicketYa necesita gestionar el proceso de compra de entradas de manera confiable, coordinando las reservas temporales, el procesamiento de pagos y la emisión de entradas mediante distintos microservicios.

Para resolver esta necesidad, se desarrollará un microservicio independiente de Compras y Pagos, encargado de registrar las operaciones comerciales, garantizar que no se procesen compras o pagos duplicados y administrar las devoluciones y reintegros correspondientes.

Este desarrollo forma parte de la primera entrega del Trabajo Práctico Final de Arquitecturas Web.

## ¿Qué funcionalidades se incorporarán?

- Implementación de la creación y consulta de compras, registrando el cliente, evento, reserva, detalle de entradas, importes y estados de cada operación.
- Comunicación con el microservicio de Eventos para consultar la información necesaria sobre los eventos, sus fechas y los precios de las entradas.
- Comunicación con el microservicio de Reservas para coordinar la confirmación y liberación de reservas temporales.
- Implementación de un sistema de pagos simulados que permita registrar operaciones aprobadas y rechazadas.
- Incorporación de mecanismos de idempotencia para evitar que las solicitudes repetidas generen compras o pagos duplicados.
- Comunicación con el microservicio de Entradas para solicitar la emisión de tickets con códigos QR una vez confirmada una compra.
- Implementación de solicitudes de devolución de entradas, verificando que se realicen con una anticipación mínima de 72 horas respecto del evento.
- Implementación de operaciones administrativas para aprobar o rechazar solicitudes de devolución.
- Gestión de reintegros simulados, coordinando la anulación de entradas y la liberación de los lugares correspondientes.
- Procesamiento de reintegros derivados de la cancelación de eventos.
- Exposición de endpoints REST para permitir que otros componentes del sistema utilicen las funcionalidades del microservicio.
- Implementación de pruebas unitarias para verificar el comportamiento de compras, pagos y devoluciones.

## Capacidades del microservicio

### Nuevas capacidades

**1. `gestion-compras`**

Permitirá crear, consultar y administrar compras de entradas, registrando sus detalles, importes, estados y las restricciones de cantidad de entradas por cliente y evento.

**2. `procesamiento-pagos`**

Permitirá procesar pagos mediante una pasarela simulada, registrar sus resultados y garantizar la idempotencia de las operaciones para evitar cobros duplicados.

**3. `coordinacion-reservas-entradas`**

Permitirá coordinar las operaciones con los microservicios de Reservas y Entradas, incluyendo la confirmación de reservas temporales, la emisión de entradas y el tratamiento de errores o vencimientos durante el proceso de compra.

**4. `gestion-devoluciones`**

Permitirá registrar solicitudes de devolución, validar los plazos establecidos, aprobarlas o rechazarlas y gestionar los reintegros correspondientes. También coordinará la anulación de entradas y la liberación de lugares cuando corresponda.

### Capacidades existentes que se modificarán

No se modificarán capacidades existentes, debido a que TicketYa se encuentra en su etapa inicial de desarrollo y todavía no posee funcionalidades implementadas.

## Impacto del desarrollo

### Código y arquitectura

Se incorporará un proyecto independiente denominado `compras-service`, desarrollado en Java mediante Spring Boot y utilizando Maven para la gestión de dependencias.

El microservicio seguirá una arquitectura en capas, separando las responsabilidades entre controladores REST, servicios, repositorios, entidades y objetos de transferencia de datos (DTO).

### Persistencia de datos

Se utilizará una base de datos relacional independiente, gestionada mediante Spring Data JPA e Hibernate.

Se almacenará la información correspondiente a compras, detalles de compra, pagos y devoluciones.

Las referencias a información administrada por otros microservicios se conservarán mediante identificadores, evitando establecer relaciones JPA directas entre bases de datos independientes.

### API REST

Se implementarán endpoints para crear y consultar compras, procesar pagos, consultar resultados, solicitar devoluciones y gestionar las operaciones administrativas de aprobación, rechazo y reintegro.

Los contratos de los endpoints se documentarán mediante OpenAPI/Swagger.

### Comunicación con otros microservicios

Compras y Pagos necesitará comunicarse mediante REST con los siguientes servicios:

- **Eventos:** para consultar información de eventos, fechas, sectores y precios.
- **Reservas:** para coordinar la confirmación y liberación de reservas y lugares.
- **Entradas:** para solicitar la emisión y anulación de tickets.

Durante la primera entrega, el procesamiento de pagos se realizará mediante una pasarela simulada, sin utilizar proveedores de pago reales.

### Pruebas y validaciones

Se desarrollarán pruebas unitarias utilizando JUnit y Mockito para verificar la lógica de negocio.

Se contemplarán, entre otros, los siguientes escenarios:

- Creación exitosa de una compra.
- Aprobación y rechazo de pagos.
- Solicitudes repetidas que no deben generar cobros duplicados.
- Intentos de compra con reservas vencidas.
- Errores de comunicación con otros microservicios.
- Solicitudes de devolución válidas e inválidas.
- Aprobación y rechazo administrativo de devoluciones.
- Procesamiento de reintegros por cancelación de eventos.

La prevención de doble venta de asientos será responsabilidad principal del microservicio de Reservas. Compras y Pagos deberá respetar sus mecanismos de control y evitar la confirmación o el cobro duplicado de operaciones.