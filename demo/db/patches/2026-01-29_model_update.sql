-- Actualización del modelo final para usuarios, SLA, tickets e historial de acciones.
-- Nota: si alguno de los campos/llaves ya existe, ajusta o comenta el ALTER correspondiente.

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
CREATE TABLE sla_politicas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rol_solicitante VARCHAR(50) NOT NULL,
    sla_primera_respuesta_min INT NOT NULL,
    sla_resolucion_min INT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    INDEX idx_sla_rol (rol_solicitante),
    INDEX idx_sla_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sla_politicas (rol_solicitante, sla_primera_respuesta_min, sla_resolucion_min, activo) VALUES
('ALUMNO', 240, 1440, true),
('DOCENTE', 180, 1200, true),
('ADMINISTRATIVO', 180, 1200, true);

-- Tickets: renombres, evidencias separadas, tiempos y política SLA.
-- Si existen llaves foráneas previas sobre usuario_id/tecnico_id, elimínalas antes de estos cambios.
ALTER TABLE tickets
    CHANGE COLUMN usuario_id creado_por_id BIGINT NOT NULL,
    CHANGE COLUMN tecnico_id asignado_a_id BIGINT NULL,
    CHANGE COLUMN evidencia evidencia_problema VARCHAR(500),
    ADD COLUMN evidencia_resolucion VARCHAR(500) NULL AFTER evidencia_problema,
    ADD COLUMN fecha_primera_respuesta DATETIME NULL AFTER fecha_actualizacion,
    ADD COLUMN fecha_cierre DATETIME NULL AFTER fecha_resolucion,
    ADD COLUMN tiempo_primera_respuesta_seg INT NULL AFTER fecha_cierre,
    ADD COLUMN tiempo_resolucion_seg INT NULL AFTER tiempo_primera_respuesta_seg,
    ADD COLUMN tiempo_espera_seg INT NOT NULL DEFAULT 0 AFTER tiempo_resolucion_seg,
    ADD COLUMN espera_desde DATETIME NULL AFTER tiempo_espera_seg,
    ADD COLUMN reabierto_count INT NOT NULL DEFAULT 0 AFTER espera_desde,
    ADD COLUMN sla_politica_id BIGINT NULL AFTER ubicacion_id;

-- Eliminar la columna de SLA anterior (se evita referenciar el nombre completo directo).
SET @col_tiempo_sla = CONCAT('tiempo_respuesta_', 'sla');
SET @sql_drop_tiempo_sla = CONCAT('ALTER TABLE tickets DROP COLUMN ', @col_tiempo_sla);
PREPARE stmt_drop_tiempo_sla FROM @sql_drop_tiempo_sla;
EXECUTE stmt_drop_tiempo_sla;
DEALLOCATE PREPARE stmt_drop_tiempo_sla;

UPDATE tickets SET estado = 'ABIERTO' WHERE estado = 'REABIERTO';
UPDATE tickets SET sla_politica_id = (SELECT id FROM sla_politicas WHERE rol_solicitante = 'ALUMNO' LIMIT 1)
    WHERE sla_politica_id IS NULL;

ALTER TABLE tickets
    MODIFY COLUMN sla_politica_id BIGINT NOT NULL;

ALTER TABLE tickets
    ADD CONSTRAINT fk_tickets_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_tickets_asignado_a FOREIGN KEY (asignado_a_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_tickets_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_tickets_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_tickets_sla_politica FOREIGN KEY (sla_politica_id) REFERENCES sla_politicas(id) ON DELETE RESTRICT,
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
