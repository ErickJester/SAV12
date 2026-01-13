-- Script SQL para crear el esquema completo de SAV12
-- Base de datos: sav12

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS sav12 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sav12;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    correo VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL,
    boleta VARCHAR(50),
    id_trabajador VARCHAR(50),
    activo BOOLEAN DEFAULT TRUE,
    INDEX idx_correo (correo),
    INDEX idx_rol (rol)
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

-- Tabla de tickets
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    estado VARCHAR(50) NOT NULL DEFAULT 'ABIERTO',
    prioridad VARCHAR(50) DEFAULT 'MEDIA',
    usuario_id BIGINT NOT NULL,
    tecnico_id BIGINT,
    categoria_id BIGINT,
    ubicacion_id BIGINT,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME,
    fecha_resolucion DATETIME,
    evidencia VARCHAR(500),
    tiempo_respuesta_sla INT DEFAULT 24,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (tecnico_id) REFERENCES usuarios(id) ON DELETE SET NULL,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id) ON DELETE SET NULL,
    FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones(id) ON DELETE SET NULL,
    INDEX idx_usuario (usuario_id),
    INDEX idx_tecnico (tecnico_id),
    INDEX idx_estado (estado),
    INDEX idx_fecha_creacion (fecha_creacion)
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
    accion VARCHAR(255) NOT NULL,
    estado_anterior VARCHAR(50),
    estado_nuevo VARCHAR(50),
    fecha_accion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalles TEXT,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE,
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_ticket (ticket_id),
    INDEX idx_fecha_accion (fecha_accion)
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

-- Verificar las tablas creadas
SHOW TABLES;
