-- Script SQL para inicializar datos básicos del sistema SAV12

-- Crear categorías de ejemplo
INSERT INTO categorias (nombre, descripcion, activo) VALUES 
('Hardware', 'Problemas relacionados con equipo físico', true),
('Software', 'Problemas con aplicaciones y sistemas operativos', true),
('Red', 'Problemas de conectividad y red', true),
('Impresoras', 'Problemas con impresoras y escáneres', true),
('Audio/Video', 'Problemas con proyectores, audio y video', true),
('Acceso', 'Problemas de acceso a sistemas y cuentas', true),
('Otro', 'Otros problemas no categorizados', true);

-- Crear ubicaciones de ejemplo
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

-- Crear usuarios de ejemplo (las contraseñas deberían estar encriptadas en producción)
-- Usuario normal
INSERT INTO usuarios (nombre, correo, password, rol, boleta, id_trabajador, activo) VALUES 
('Juan Pérez', 'juan.perez@example.com', 'password123', 'USUARIO', '2021600001', NULL, true);

-- Técnico
INSERT INTO usuarios (nombre, correo, password, rol, boleta, id_trabajador, activo) VALUES 
('María García', 'maria.garcia@example.com', 'tecnico123', 'TECNICO', NULL, 'TEC001', true),
('Carlos López', 'carlos.lopez@example.com', 'tecnico123', 'TECNICO', NULL, 'TEC002', true);

-- Administrador
INSERT INTO usuarios (nombre, correo, password, rol, boleta, id_trabajador, activo) VALUES 
('Admin Sistema', 'admin@example.com', 'admin123', 'ADMIN', NULL, 'ADM001', true);

-- Nota: En producción, las contraseñas deben estar encriptadas con BCrypt
-- Ejemplo de contraseña encriptada: $2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlWXx2lPk1C3G6
