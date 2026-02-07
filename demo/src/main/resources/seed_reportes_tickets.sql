-- seed_reportes_tickets.sql
-- Objetivo: poblar la tabla tickets con datos variados (estados + fechas) para probar /admin/reportes
-- Idempotente: borra SOLO tickets con titulo que empiece por 'SEED-' y los vuelve a crear.

USE sav12_app;

-- =========================
-- 0) Validaciones mínimas
-- =========================
SET @juan   := (SELECT id FROM usuarios WHERE correo='juan.perez@example.com'   LIMIT 1);
SET @maria  := (SELECT id FROM usuarios WHERE correo='maria.garcia@example.com' LIMIT 1);
SET @carlos := (SELECT id FROM usuarios WHERE correo='carlos.lopez@example.com' LIMIT 1);
SET @admin  := (SELECT id FROM usuarios WHERE correo='admin@example.com'        LIMIT 1);

-- Si alguno es NULL, este script va a fallar por FK/NOT NULL. Verifica que existan esos usuarios.
SELECT
  IFNULL(@juan,   0) AS juan_id,
  IFNULL(@maria,  0) AS maria_id,
  IFNULL(@carlos, 0) AS carlos_id,
  IFNULL(@admin,  0) AS admin_id;

-- =========================
-- 1) SLA policies (asegura que existan para roles que vamos a usar)
-- =========================
INSERT INTO sla_politicas (rol_solicitante, sla_primera_respuesta_min, sla_resolucion_min, activo) VALUES
('ALUMNO', 240, 1440, true),
('TECNICO', 120, 720, true),
('ADMIN', 120, 720, true)
ON DUPLICATE KEY UPDATE
  sla_primera_respuesta_min = VALUES(sla_primera_respuesta_min),
  sla_resolucion_min        = VALUES(sla_resolucion_min),
  activo                    = VALUES(activo);

SET @sla_alumno := (SELECT id FROM sla_politicas WHERE rol_solicitante='ALUMNO'  LIMIT 1);
SET @sla_tecnico:= (SELECT id FROM sla_politicas WHERE rol_solicitante='TECNICO' LIMIT 1);
SET @sla_admin  := (SELECT id FROM sla_politicas WHERE rol_solicitante='ADMIN'   LIMIT 1);

-- =========================
-- 2) Cat/Ubic ids (toma algunas existentes del schema.sql)
-- =========================
SET @cat_hw  := (SELECT id FROM categorias  WHERE nombre='Hardware'   LIMIT 1);
SET @cat_sw  := (SELECT id FROM categorias  WHERE nombre='Software'   LIMIT 1);
SET @cat_red := (SELECT id FROM categorias  WHERE nombre='Red'        LIMIT 1);

SET @ub_lab1 := (SELECT id FROM ubicaciones WHERE edificio='Edificio Norte'   AND salon='Laboratorio 1' LIMIT 1);
SET @ub_101  := (SELECT id FROM ubicaciones WHERE edificio='Edificio Central' AND salon='Sala 101'      LIMIT 1);
SET @ub_bibl := (SELECT id FROM ubicaciones WHERE edificio='Edificio Sur'     AND salon='Biblioteca'    LIMIT 1);

-- =========================
-- 3) Limpieza de seeds anteriores
-- =========================
DELETE FROM tickets WHERE titulo LIKE 'SEED-%';

-- =========================
-- 4) Fechas base (relativas a NOW())
-- =========================
-- semana (dentro de 7 días)
SET @d_2d  := DATE_SUB(NOW(), INTERVAL 2 DAY);
SET @d_6d  := DATE_SUB(NOW(), INTERVAL 6 DAY);

-- mes (dentro de 30 días)
SET @d_20d := DATE_SUB(NOW(), INTERVAL 20 DAY);

-- ~2 meses
SET @d_40d := DATE_SUB(NOW(), INTERVAL 40 DAY);

-- ~3 meses
SET @d_80d := DATE_SUB(NOW(), INTERVAL 80 DAY);

-- ~7 meses
SET @d_200d := DATE_SUB(NOW(), INTERVAL 200 DAY);

-- > 1 año (para que NO entre en "anual")
SET @d_400d := DATE_SUB(NOW(), INTERVAL 400 DAY);

-- =========================
-- 5) Inserts
-- =========================
-- Nota: ReporteService cuenta SLA SOLO si estado RESUELTO o CERRADO y fecha_resolucion != NULL.

INSERT INTO tickets (
  titulo, descripcion, estado, prioridad,
  creado_por_id, asignado_a_id,
  categoria_id, ubicacion_id, sla_politica_id,
  fecha_creacion, fecha_actualizacion,
  fecha_primera_respuesta, fecha_resolucion, fecha_cierre,
  evidencia_problema, evidencia_resolucion,
  tiempo_primera_respuesta_seg, tiempo_resolucion_seg,
  tiempo_espera_seg, espera_desde, reabierto_count
) VALUES

-- 1) ABIERTO (reciente)
('SEED-01 ABIERTO (2d)', 'Ticket abierto reciente para probar conteos.', 'ABIERTO', 'MEDIA',
 @juan, @maria, @cat_sw, @ub_101, @sla_alumno,
 @d_2d, @d_2d,
 NULL, NULL, NULL,
 NULL, NULL,
 NULL, NULL,
 0, NULL, 0),

-- 2) EN_PROCESO (reciente, con primera respuesta)
('SEED-02 EN_PROCESO (6d)', 'En proceso, ya tuvo primera respuesta.', 'EN_PROCESO', 'ALTA',
 @juan, @carlos, @cat_hw, @ub_lab1, @sla_alumno,
 @d_6d, DATE_ADD(@d_6d, INTERVAL 1 DAY),
 DATE_ADD(@d_6d, INTERVAL 30 MINUTE), NULL, NULL,
 NULL, NULL,
 1800, NULL,
 0, NULL, 0),

-- 3) EN_ESPERA (20d) con espera_desde y tiempo_espera_seg acumulado
('SEED-03 EN_ESPERA (20d)', 'En espera con acumulado de espera.', 'EN_ESPERA', 'MEDIA',
 @juan, @maria, @cat_red, @ub_bibl, @sla_alumno,
 @d_20d, DATE_ADD(@d_20d, INTERVAL 10 DAY),
 DATE_ADD(@d_20d, INTERVAL 2 HOUR), NULL, NULL,
 NULL, NULL,
 7200, NULL,
 86400, DATE_ADD(@d_20d, INTERVAL 10 DAY), 0),

-- 4) RESUELTO (20d) CUMPLE SLA (primera 1h, resol 8h)
('SEED-04 RESUELTO OK (20d)', 'Resuelto y cumple SLA.', 'RESUELTO', 'MEDIA',
 @juan, @carlos, @cat_sw, @ub_101, @sla_alumno,
 @d_20d, DATE_ADD(@d_20d, INTERVAL 9 HOUR),
 DATE_ADD(@d_20d, INTERVAL 1 HOUR),
 DATE_ADD(@d_20d, INTERVAL 8 HOUR),
 NULL,
 'evidencia-problema.jpg', 'evidencia-resolucion.jpg',
 3600, 28800,
 0, NULL, 0),

-- 5) RESUELTO (40d) FALLA primera respuesta (10h), resol 12h
('SEED-05 RESUELTO FAIL PR (40d)', 'Resuelto pero primera respuesta tarde.', 'RESUELTO', 'ALTA',
 @juan, @maria, @cat_hw, @ub_lab1, @sla_alumno,
 @d_40d, DATE_ADD(@d_40d, INTERVAL 13 HOUR),
 DATE_ADD(@d_40d, INTERVAL 10 HOUR),
 DATE_ADD(@d_40d, INTERVAL 12 HOUR),
 NULL,
 NULL, NULL,
 36000, 43200,
 0, NULL, 0),

-- 6) CERRADO (40d) con cierre, CUMPLE resol considerando espera (50h total, 20h espera => 30h efectivo)
('SEED-06 CERRADO OK con espera (40d)', 'Cerrado; resolución incluye espera descontable.', 'CERRADO', 'MEDIA',
 @juan, @carlos, @cat_red, @ub_bibl, @sla_alumno,
 @d_40d, DATE_ADD(@d_40d, INTERVAL 60 HOUR),
 DATE_ADD(@d_40d, INTERVAL 2 HOUR),
 DATE_ADD(@d_40d, INTERVAL 50 HOUR),
 DATE_ADD(@d_40d, INTERVAL 60 HOUR),
 NULL, NULL,
 7200, 180000,
 72000, DATE_ADD(@d_40d, INTERVAL 10 HOUR), 0),

-- 7) CANCELADO (80d)
('SEED-07 CANCELADO (80d)', 'Cancelado; no cuenta para SLA.', 'CANCELADO', 'BAJA',
 @juan, NULL, @cat_sw, @ub_101, @sla_alumno,
 @d_80d, DATE_ADD(@d_80d, INTERVAL 1 DAY),
 NULL, NULL, NULL,
 NULL, NULL,
 NULL, NULL,
 0, NULL, 0),

-- 8) REABIERTO (80d) con reabierto_count>0 (para probar conteo reabiertos)
('SEED-08 REABIERTO (80d)', 'Reabierto para probar conteo y estado activo.', 'REABIERTO', 'ALTA',
 @juan, @maria, @cat_hw, @ub_lab1, @sla_alumno,
 @d_80d, DATE_SUB(NOW(), INTERVAL 1 DAY),
 DATE_ADD(@d_80d, INTERVAL 1 HOUR),
 DATE_ADD(@d_80d, INTERVAL 20 HOUR),  -- tuvo una resolución previa (no cuenta para SLA porque estado != RESUELTO/CERRADO)
 NULL,
 NULL, NULL,
 3600, 72000,
 0, NULL, 1),

-- 9) RESUELTO (200d) creado por TECNICO (para que también caiga en reportes) y con SLA TECNICO
('SEED-09 RESUELTO TECNICO (200d)', 'Resuelto por técnico; SLA técnico.', 'RESUELTO', 'MEDIA',
 @maria, @maria, @cat_sw, @ub_101, @sla_tecnico,
 @d_200d, DATE_ADD(@d_200d, INTERVAL 6 HOUR),
 DATE_ADD(@d_200d, INTERVAL 30 MINUTE),
 DATE_ADD(@d_200d, INTERVAL 5 HOUR),
 NULL,
 NULL, NULL,
 1800, 18000,
 0, NULL, 0),

-- 10) CERRADO (400d) creado por ADMIN (para NO entrar en anual)
('SEED-10 CERRADO ADMIN (400d)', 'Muy viejo (>1 año). No debe salir en anual.', 'CERRADO', 'MEDIA',
 @admin, @carlos, NULL, NULL, @sla_admin,
 @d_400d, DATE_ADD(@d_400d, INTERVAL 30 HOUR),
 DATE_ADD(@d_400d, INTERVAL 1 HOUR),
 DATE_ADD(@d_400d, INTERVAL 20 HOUR),
 DATE_ADD(@d_400d, INTERVAL 30 HOUR),
 NULL, NULL,
 3600, 72000,
 0, NULL, 0);

-- =========================
-- 6) Resumen rápido (debug)
-- =========================
SELECT estado, COUNT(*) AS cantidad
FROM tickets
WHERE titulo LIKE 'SEED-%'
GROUP BY estado
ORDER BY estado;

SELECT MIN(fecha_creacion) AS min_creacion, MAX(fecha_creacion) AS max_creacion
FROM tickets
WHERE titulo LIKE 'SEED-%';
