-- Actualización del modelo final para usuarios, SLA, tickets e historial de acciones.
-- Nota: Ajusta los nombres de llaves foráneas si tu base usa otros nombres reales.

-- Eliminar llaves foráneas anteriores que bloqueen renombres.
-- Si necesitas más control, consulta INFORMATION_SCHEMA para ver las llaves actuales.
SET @drop_tickets_fk = (
    SELECT CONCAT(
        'ALTER TABLE tickets ',
        GROUP_CONCAT(CONCAT('DROP FOREIGN KEY ', CONSTRAINT_NAME) SEPARATOR ', ')
    )
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'tickets'
      AND referenced_table_name IS NOT NULL
      AND column_name IN ('usuario_id', 'tecnico_id', 'categoria_id', 'ubicacion_id',
                          'creado_por_id', 'asignado_a_id', 'sla_politica_id')
);
SET @drop_tickets_fk = IFNULL(@drop_tickets_fk, 'SELECT 1');
PREPARE stmt_drop_tickets_fk FROM @drop_tickets_fk;
EXECUTE stmt_drop_tickets_fk;
DEALLOCATE PREPARE stmt_drop_tickets_fk;

SET @drop_historial_fk = (
    SELECT CONCAT(
        'ALTER TABLE historial_acciones ',
        GROUP_CONCAT(CONCAT('DROP FOREIGN KEY ', CONSTRAINT_NAME) SEPARATOR ', ')
    )
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'historial_acciones'
      AND referenced_table_name IS NOT NULL
      AND column_name IN ('ticket_id', 'usuario_id', 'asignado_anterior_id', 'asignado_nuevo_id')
);
SET @drop_historial_fk = IFNULL(@drop_historial_fk, 'SELECT 1');
PREPARE stmt_drop_historial_fk FROM @drop_historial_fk;
EXECUTE stmt_drop_historial_fk;
DEALLOCATE PREPARE stmt_drop_historial_fk;

-- Usuarios: hash de contraseña, roles nuevos e índices únicos.
ALTER TABLE usuarios
    CHANGE COLUMN password password_hash VARCHAR(255) NOT NULL;

UPDATE usuarios SET rol = 'ALUMNO' WHERE rol = 'USUARIO';
UPDATE usuarios SET rol = 'ADMINISTRATIVO' WHERE rol IN ('COORDINADOR', 'SUPERVISOR');

ALTER TABLE usuarios
    ADD UNIQUE KEY uq_usuarios_boleta (boleta),
    ADD UNIQUE KEY uq_usuarios_id_trabajador (id_trabajador),
    ADD CONSTRAINT chk_usuarios_rol CHECK (rol IN ('ALUMNO', 'DOCENTE', 'ADMINISTRATIVO', 'TECNICO', 'ADMIN'));

-- Tabla de políticas SLA.
CREATE TABLE IF NOT EXISTS sla_politicas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rol_solicitante VARCHAR(50) NOT NULL,
    sla_primera_respuesta_min INT NOT NULL,
    sla_resolucion_min INT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uq_sla_rol (rol_solicitante),
    INDEX idx_sla_rol (rol_solicitante),
    INDEX idx_sla_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sla_politicas (rol_solicitante, sla_primera_respuesta_min, sla_resolucion_min, activo) VALUES
('ALUMNO', 240, 1440, true),
('DOCENTE', 180, 1200, true),
('ADMINISTRATIVO', 180, 1200, true)
ON DUPLICATE KEY UPDATE
    sla_primera_respuesta_min = VALUES(sla_primera_respuesta_min),
    sla_resolucion_min = VALUES(sla_resolucion_min),
    activo = VALUES(activo);

-- Tickets: renombres, evidencias separadas, tiempos y política SLA.
ALTER TABLE tickets
    CHANGE COLUMN usuario_id creado_por_id BIGINT NOT NULL,
    CHANGE COLUMN tecnico_id asignado_a_id BIGINT NULL,
    CHANGE COLUMN evidencia evidencia_problema VARCHAR(500),
    MODIFY COLUMN estado ENUM('ABIERTO', 'REABIERTO', 'EN_PROCESO', 'EN_ESPERA', 'RESUELTO', 'CERRADO', 'CANCELADO') NOT NULL DEFAULT 'ABIERTO',
    ADD COLUMN evidencia_resolucion VARCHAR(500) NULL AFTER evidencia_problema,
    ADD COLUMN fecha_primera_respuesta DATETIME NULL AFTER fecha_actualizacion,
    ADD COLUMN fecha_cierre DATETIME NULL AFTER fecha_resolucion,
    ADD COLUMN tiempo_primera_respuesta_seg INT NULL AFTER fecha_cierre,
    ADD COLUMN tiempo_resolucion_seg INT NULL AFTER tiempo_primera_respuesta_seg,
    ADD COLUMN tiempo_espera_seg INT NOT NULL DEFAULT 0 AFTER tiempo_resolucion_seg,
    ADD COLUMN espera_desde DATETIME NULL AFTER tiempo_espera_seg,
    ADD COLUMN reabierto_count INT NOT NULL DEFAULT 0 AFTER espera_desde,
    ADD COLUMN sla_politica_id BIGINT NULL,
    DROP COLUMN tiempo_respuesta_sla;

UPDATE tickets SET sla_politica_id = (SELECT id FROM sla_politicas WHERE rol_solicitante = 'ALUMNO' LIMIT 1)
    WHERE sla_politica_id IS NULL;

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_tickets_asignado_a FOREIGN KEY (asignado_a_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_tickets_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_tickets_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_tickets_sla_politica FOREIGN KEY (sla_politica_id) REFERENCES sla_politicas(id) ON DELETE SET NULL,
    ADD INDEX idx_creado_por (creado_por_id),
    ADD INDEX idx_asignado_a (asignado_a_id),
    ADD INDEX idx_sla_politica (sla_politica_id);

-- Historial de acciones: tipo y cambios de asignación.
ALTER TABLE historial_acciones
    ADD COLUMN tipo VARCHAR(50) NOT NULL DEFAULT 'ACTUALIZACION' AFTER usuario_id,
    ADD COLUMN asignado_anterior_id BIGINT NULL AFTER estado_nuevo,
    ADD COLUMN asignado_nuevo_id BIGINT NULL AFTER asignado_anterior_id,
    ADD CONSTRAINT fk_historial_asignado_anterior FOREIGN KEY (asignado_anterior_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_historial_asignado_nuevo FOREIGN KEY (asignado_nuevo_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    ADD INDEX idx_tipo (tipo),
    ADD INDEX idx_asignado_anterior (asignado_anterior_id),
    ADD INDEX idx_asignado_nuevo (asignado_nuevo_id);
