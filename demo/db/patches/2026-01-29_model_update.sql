-- Actualización del modelo para usuarios, SLA, tickets e historial de acciones.

-- Usuarios: hash de contraseña, roles nuevos e índices únicos.
ALTER TABLE usuarios
    CHANGE COLUMN password password_hash VARCHAR(255) NOT NULL,
    ADD UNIQUE KEY uq_usuarios_boleta (boleta),
    ADD UNIQUE KEY uq_usuarios_id_trabajador (id_trabajador),
    ADD CONSTRAINT chk_usuarios_rol CHECK (rol IN ('USUARIO', 'TECNICO', 'ADMIN', 'COORDINADOR', 'SUPERVISOR'));

-- Tabla de políticas SLA.
CREATE TABLE IF NOT EXISTS sla_politicas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    tiempo_respuesta_horas INT NOT NULL,
    tiempo_resolucion_horas INT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sla_politicas (nombre, descripcion, tiempo_respuesta_horas, tiempo_resolucion_horas, activo) VALUES
('ALTA', 'Prioridad alta con respuesta rápida', 4, 24, true),
('MEDIA', 'Prioridad estándar para la mayoría de los tickets', 8, 48, true),
('BAJA', 'Prioridad baja con tiempos extendidos', 24, 120, true)
ON DUPLICATE KEY UPDATE nombre=nombre;

-- Tickets: nuevas referencias, evidencias separadas, tiempos y política SLA.
ALTER TABLE tickets
    DROP FOREIGN KEY tickets_ibfk_1,
    DROP FOREIGN KEY tickets_ibfk_2,
    DROP FOREIGN KEY tickets_ibfk_3,
    DROP FOREIGN KEY tickets_ibfk_4,
    DROP INDEX idx_usuario,
    DROP INDEX idx_tecnico,
    DROP COLUMN usuario_id,
    DROP COLUMN tecnico_id,
    DROP COLUMN evidencia,
    DROP COLUMN tiempo_respuesta_sla,
    ADD COLUMN creado_por_id BIGINT NOT NULL AFTER prioridad,
    ADD COLUMN asignado_a_id BIGINT NULL AFTER creado_por_id,
    ADD COLUMN sla_politica_id BIGINT NOT NULL AFTER ubicacion_id,
    ADD COLUMN fecha_asignacion DATETIME NULL AFTER fecha_actualizacion,
    ADD COLUMN fecha_primera_respuesta DATETIME NULL AFTER fecha_asignacion,
    ADD COLUMN evidencia_usuario VARCHAR(500) NULL AFTER fecha_resolucion,
    ADD COLUMN evidencia_tecnico VARCHAR(500) NULL AFTER evidencia_usuario,
    ADD COLUMN tiempo_respuesta_minutos INT NULL AFTER evidencia_tecnico,
    ADD COLUMN tiempo_resolucion_minutos INT NULL AFTER tiempo_respuesta_minutos,
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
