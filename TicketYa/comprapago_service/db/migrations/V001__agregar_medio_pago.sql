-- Migracion manual para MySQL 8. No se ejecuta al iniciar Spring Boot.
-- Seleccionar explicitamente la base destino y respaldar pagos antes de aplicar.
-- Ejecutar el archivo completo en una misma conexion, sin migradores concurrentes.
-- Requiere una tabla pagos existente. No crea bases/tablas ni modifica registros.
-- Si la columna existe, no cambia su tipo, nulabilidad ni contenido.

SET @ticketya_medio_pago_ddl = IF(
    EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'pagos'
          AND COLUMN_NAME = 'medio_pago'
    ),
    'SELECT ''medio_pago ya existe; no se modifica. Verificar su definicion.'' AS resultado',
    'ALTER TABLE `pagos` ADD COLUMN `medio_pago` VARCHAR(32) NULL'
);

PREPARE ticketya_medio_pago_stmt FROM @ticketya_medio_pago_ddl;
EXECUTE ticketya_medio_pago_stmt;
DEALLOCATE PREPARE ticketya_medio_pago_stmt;
SET @ticketya_medio_pago_ddl = NULL;

-- Verificacion de estructura: VARCHAR(32), IS_NULLABLE = YES, COLUMN_DEFAULT = NULL.
-- Una columna preexistente puede tener otra definicion: revisar antes de continuar.
SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, COLUMN_TYPE,
       IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'pagos'
  AND COLUMN_NAME = 'medio_pago';
