-- Script SQL idempotente para inicializar datos básicos del sistema SAV12
-- (puede ejecutarse múltiples veces sin tirar Duplicate entry)

START TRANSACTION;

-- =========================
-- CATEGORÍAS (UPSERT por UNIQUE(nombre))
-- =========================
INSERT INTO categorias (nombre, descripcion, activo)
VALUES
  ('Hardware',  'Problemas relacionados con equipo físico',                TRUE),
  ('Software',  'Problemas con aplicaciones y sistemas operativos',        TRUE),
  ('Red',       'Problemas de conectividad y red',                         TRUE),
  ('Impresoras','Problemas con impresoras y escáneres',                    TRUE),
  ('Audio/Video','Problemas con proyectores, audio y video',               TRUE),
  ('Acceso',    'Problemas de acceso a sistemas y cuentas',                TRUE),
  ('Otro',      'Otros problemas no categorizados',                        TRUE)
AS new_row
ON DUPLICATE KEY UPDATE
  descripcion = new_row.descripcion,
  activo      = new_row.activo;

-- =========================
-- UBICACIONES (INSERT si NO existe por (edificio,piso,salon))
-- =========================
INSERT INTO ubicaciones (edificio, piso, salon, activo)
SELECT v.edificio, v.piso, v.salon, v.activo
FROM (
  SELECT 'Edificio Central' AS edificio, 'Planta Baja' AS piso, 'Sala 101' AS salon, TRUE AS activo
  UNION ALL SELECT 'Edificio Central', 'Planta Baja', 'Sala 102', TRUE
  UNION ALL SELECT 'Edificio Central', 'Primer Piso',  'Sala 201', TRUE
  UNION ALL SELECT 'Edificio Central', 'Primer Piso',  'Sala 202', TRUE
  UNION ALL SELECT 'Edificio Central', 'Segundo Piso', 'Sala 301', TRUE
  UNION ALL SELECT 'Edificio Central', 'Segundo Piso', 'Sala 302', TRUE
  UNION ALL SELECT 'Edificio Norte',   'Planta Baja',  'Laboratorio 1', TRUE
  UNION ALL SELECT 'Edificio Norte',   'Planta Baja',  'Laboratorio 2', TRUE
  UNION ALL SELECT 'Edificio Norte',   'Primer Piso',  'Aula Magna', TRUE
  UNION ALL SELECT 'Edificio Sur',     'Planta Baja',  'Biblioteca', TRUE
  UNION ALL SELECT 'Edificio Sur',     'Primer Piso',  'Sala de Profesores', TRUE
  UNION ALL SELECT 'Edificio Oeste',   'Planta Baja',  'Cafetería', TRUE
  UNION ALL SELECT 'Edificio Oeste',   'Primer Piso',  'Auditorio', TRUE
) AS v
WHERE NOT EXISTS (
  SELECT 1
  FROM ubicaciones u
  WHERE u.edificio = v.edificio
    AND u.piso     = v.piso
    AND u.salon    = v.salon
);

-- =========================
-- USUARIOS (UPSERT por UNIQUE(correo) y/o boleta/id_trabajador)
-- Nota: contraseñas hasheadas con BCrypt (cost 12) para local
-- =========================
INSERT INTO usuarios (nombre, correo, password_hash, rol, boleta, id_trabajador, activo)
VALUES
  ('Juan Pérez',   'juan.perez@example.com',   '$2b$12$dX/uczi3R0oRBBdVTC2o6.JU1E/.FzyHSq0unoUwcECaHAI7LnIli', 'ALUMNO',  '2021600001', NULL,     TRUE),
  ('María García', 'maria.garcia@example.com', '$2b$12$fBKZoUpionSpTY8gz/GiJ.s4Jye5BwcVGrQbnSYkgL5qVgHjsfxZy',  'TECNICO', NULL,         'TEC001', TRUE),
  ('Carlos López', 'carlos.lopez@example.com', '$2b$12$fBKZoUpionSpTY8gz/GiJ.s4Jye5BwcVGrQbnSYkgL5qVgHjsfxZy',  'TECNICO', NULL,         'TEC002', TRUE),
  ('Admin Sistema','admin@example.com',        '$2b$12$k2WHo/zzUJAeatXzkg0FluI7zQDvj/U/KiwJ9sc2MxMU7zxil4AJ6',    'ADMIN',   NULL,         'ADM001', TRUE)
AS new_row
ON DUPLICATE KEY UPDATE
  nombre        = new_row.nombre,
  password_hash = new_row.password_hash,
  rol           = new_row.rol,
  boleta        = new_row.boleta,
  id_trabajador = new_row.id_trabajador,
  activo        = new_row.activo;

COMMIT;
