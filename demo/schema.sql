-- Script SQL para crear el esquema completo de SAV12
-- Base de datos: sav12_app

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS sav12_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sav12_app;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    boleta VARCHAR(50),
    id_trabajador VARCHAR(50),
    activo BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uq_usuarios_boleta (boleta),
    UNIQUE KEY uq_usuarios_id_trabajador (id_trabajador),
    INDEX idx_correo (correo),
    INDEX idx_rol (rol),
    CONSTRAINT chk_usuarios_rol CHECK (rol IN ('ALUMNO', 'DOCENTE', 'ADMINISTRATIVO', 'TECNICO', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de categorías
CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL UNIQUE,
    descripcion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    INDEX idx_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de ubicaciones
CREATE TABLE IF NOT EXISTS ubicaciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    edificio VARCHAR(255) NOT NULL,
    piso VARCHAR(100),
    salon VARCHAR(100),
    activo BOOLEAN DEFAULT TRUE,
    INDEX idx_edificio (edificio),
    INDEX idx_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de políticas de SLA
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

-- Tabla de tickets
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    estado ENUM('ABIERTO', 'REABIERTO', 'EN_PROCESO', 'EN_ESPERA', 'RESUELTO', 'CERRADO', 'CANCELADO') NOT NULL DEFAULT 'ABIERTO',
    prioridad VARCHAR(50) DEFAULT 'MEDIA',
    creado_por_id BIGINT NOT NULL,
    asignado_a_id BIGINT,
    categoria_id BIGINT,
    ubicacion_id BIGINT,
    sla_politica_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME,
    fecha_primera_respuesta DATETIME,
    fecha_resolucion DATETIME,
    fecha_cierre DATETIME,
    evidencia_problema VARCHAR(500),
    evidencia_resolucion VARCHAR(500),
    tiempo_primera_respuesta_seg INT,
    tiempo_resolucion_seg INT,
    tiempo_espera_seg INT NOT NULL DEFAULT 0,
    espera_desde DATETIME,
    reabierto_count INT NOT NULL DEFAULT 0,
    FOREIGN KEY (creado_por_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (asignado_a_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL,
    FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones(id) ON DELETE SET NULL,
    FOREIGN KEY (sla_politica_id) REFERENCES sla_politicas(id) ON DELETE RESTRICT,
    INDEX idx_creado_por (creado_por_id),
    INDEX idx_asignado_a (asignado_a_id),
    INDEX idx_estado (estado),
    INDEX idx_fecha_creacion (fecha_creacion),
    INDEX idx_sla_politica (sla_politica_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de comentarios
CREATE TABLE IF NOT EXISTS comentarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    contenido TEXT NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_ticket (ticket_id),
    INDEX idx_fecha_creacion (fecha_creacion)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de historial de acciones
CREATE TABLE IF NOT EXISTS historial_acciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    accion VARCHAR(255) NOT NULL,
    estado_anterior VARCHAR(50),
    estado_nuevo VARCHAR(50),
    asignado_anterior_id BIGINT,
    asignado_nuevo_id BIGINT,
    fecha_accion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalles TEXT,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (asignado_anterior_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    FOREIGN KEY (asignado_nuevo_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    INDEX idx_ticket (ticket_id),
    INDEX idx_fecha_accion (fecha_accion),
    INDEX idx_tipo (tipo),
    INDEX idx_asignado_anterior (asignado_anterior_id),
    INDEX idx_asignado_nuevo (asignado_nuevo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar categorías de ejemplo
INSERT INTO categorias (nombre, descripcion, activo) VALUES 
('Hardware', 'Problemas relacionados con equipo físico', true),
('Software', 'Problemas con aplicaciones y sistemas operativos', true),
('Red', 'Problemas de conectividad y red', true),
('Impresoras', 'Problemas con impresoras y escáneres', true),
('Audio/Video', 'Problemas con proyectores, audio y video', true),
('Acceso', 'Problemas de acceso a sistemas y cuentas', true),
('Otro', 'Otros problemas no categorizados', true)
ON DUPLICATE KEY UPDATE nombre=nombre;

-- Insertar ubicaciones de ejemplo
INSERT INTO ubicaciones (edificio, piso, salon, activo) VALUES 
('Edificio Central', 'Planta Baja', 'Sala 101', true),
('Edificio Central', 'Planta Baja', 'Sala 102', true),
('Edificio Central', 'Primer Piso', 'Sala 201', true),
('Edificio Central', 'Primer Piso', 'Sala 202', true),
('Edificio Central', 'Segundo Piso', 'Sala 301', true),
('Edificio Central', 'Segundo Piso', 'Sala 302', true),
('Edificio Norte', 'Planta Baja', 'Laboratorio 1', true),
('Edificio Norte', 'Planta Baja', 'Laboratorio 2', true),
('Edificio Norte', 'Primer Piso', 'Aula Magna', true),
('Edificio Sur', 'Planta Baja', 'Biblioteca', true),
('Edificio Sur', 'Primer Piso', 'Sala de Profesores', true),
('Edificio Oeste', 'Planta Baja', 'Cafetería', true),
('Edificio Oeste', 'Primer Piso', 'Auditorio', true);

-- Insertar políticas SLA de ejemplo
INSERT INTO sla_politicas (rol_solicitante, sla_primera_respuesta_min, sla_resolucion_min, activo) VALUES
('ALUMNO', 240, 1440, true),
('DOCENTE', 180, 1200, true),
('ADMINISTRATIVO', 180, 1200, true)
ON DUPLICATE KEY UPDATE rol_solicitante=rol_solicitante;

-- Verificar las tablas creadas
SHOW TABLES;
