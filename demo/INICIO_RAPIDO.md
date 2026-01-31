# Guía Rápida de Inicio - SAV12

## 🚀 Inicio Rápido

### 1. Prerrequisitos
- Java 17 o superior
- MySQL 8.0 o superior
- Maven 3.6+

### 2. Configuración de Base de Datos

La configuración está en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sav12_app
spring.datasource.username=root
spring.datasource.password=root
```

Si usas una base de datos local, cambia a:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sav12_app
spring.datasource.username=tu_usuario
spring.datasource.password=tu_contraseña
spring.jpa.hibernate.ddl-auto=none
```

### 3. Crear Base de Datos

```sql
CREATE DATABASE IF NOT EXISTS sav12_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 4. Ejecutar Script de Inicialización (Opcional)

Ejecuta el script `src/main/resources/data-init.sql` para crear datos de ejemplo:
- Categorías predefinidas
- Ubicaciones de ejemplo
- Usuarios de prueba

### 5. Ejecutar el Proyecto

```bash
# Opción 1: Con Maven Wrapper
./mvnw spring-boot:run

# Opción 2: Con Maven instalado
mvn spring-boot:run

# Opción 3: Compilar y ejecutar JAR
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### 6. Acceder al Sistema

Abre tu navegador en: **http://localhost:8080**

## 👥 Usuarios de Prueba

Si ejecutaste el script de inicialización, puedes usar:

### Usuario Normal
- **Correo**: juan.perez@example.com
- **Contraseña**: password123
- **Acceso**: Panel de usuario, crear tickets

### Técnico
- **Correo**: maria.garcia@example.com
- **Contraseña**: tecnico123
- **Acceso**: Panel técnico, gestionar tickets

### Administrador
- **Correo**: admin@example.com
- **Contraseña**: admin123
- **Acceso**: Panel admin, todas las funciones

## 📋 Funcionalidades por Rol

### 👤 Usuario
1. Ir a: http://localhost:8080/usuario/panel
2. Crear nuevo ticket: "Crear Ticket"
3. Ver mis tickets: "Mis Tickets"
4. Ver detalles y agregar comentarios
5. Reabrir tickets si están cerrados

### 🔧 Técnico
1. Ir a: http://localhost:8080/tecnico/panel
2. Ver tickets disponibles para asignar
3. Asignarme tickets
4. Cambiar estado de tickets
5. Agregar comentarios técnicos

### 👨‍💼 Administrador
1. Ir a: http://localhost:8080/admin/panel
2. Gestionar usuarios y roles
3. Asignar técnicos a tickets
4. Crear categorías y ubicaciones
5. Ver reportes de SLA
6. Exportar reportes

## 🔄 Flujo Típico de un Ticket

1. **Usuario crea ticket**
   - Rellena formulario con problema
   - Selecciona categoría y ubicación
   - Adjunta evidencia (opcional)

2. **Técnico se asigna**
   - Ve el ticket en "disponibles"
   - Se asigna el ticket
   - Cambia estado a "EN_PROCESO"

3. **Técnico resuelve**
   - Agrega comentarios con solución
   - Cambia estado a "RESUELTO"

4. **Usuario confirma o reabre**
   - Ve la solución
   - Puede agregar comentarios
   - Si no está satisfecho, reabre el ticket

5. **Ticket cerrado**
   - Admin o técnico cierra definitivamente
   - Genera métricas para SLA

## 🗂️ Estructura de URLs

### Públicas
- `/` → Redirecciona a login
- `/login` → Página de inicio de sesión
- `/registro` → Registrar nuevo usuario
- `/logout` → Cerrar sesión

### Usuario (`/usuario/*`)
- `/usuario/panel` → Dashboard
- `/usuario/crear-ticket` → Nuevo ticket
- `/usuario/mis-tickets` → Lista de tickets
- `/usuario/ticket/{id}` → Detalle del ticket

### Técnico (`/tecnico/*`)
- `/tecnico/panel` → Dashboard
- `/tecnico/mis-tickets` → Tickets asignados
- `/tecnico/ticket/{id}` → Detalle y cambio de estado

### Administrador (`/admin/*`)
- `/admin/panel` → Dashboard con estadísticas
- `/admin/usuarios` → Gestión de usuarios
- `/admin/tickets` → Todos los tickets
- `/admin/categorias` → Gestión de categorías
- `/admin/ubicaciones` → Gestión de ubicaciones
- `/admin/reportes` → Reportes de SLA

## ⚙️ Configuración Adicional

### Cambiar Puerto
En `application.properties`:
```properties
server.port=8081
```

### Habilitar SQL Debug
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### Modo de Desarrollo
```properties
spring.devtools.restart.enabled=true
spring.thymeleaf.cache=false
```

## 🐛 Solución de Problemas

### Error: "Cannot connect to database"
- Verifica que MySQL esté ejecutándose
- Confirma credenciales en `application.properties`
- Asegúrate de que la base de datos existe

### Error: "Port 8080 already in use"
- Cambia el puerto en `application.properties`
- O detén el proceso que usa el puerto 8080

### Error: "Bean not found"
- Ejecuta `mvn clean install`
- Verifica que todas las dependencias estén descargadas

### Las vistas no cargan CSS
- Verifica que los archivos CSS estén en `src/main/resources/static/css/`
- Limpia caché del navegador (Ctrl+Shift+R)
- Revisa la consola del navegador por errores

## 📚 Recursos Adicionales

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
- [MySQL Documentation](https://dev.mysql.com/doc/)

## 🔐 Seguridad (Producción)

⚠️ **IMPORTANTE**: Antes de llevar a producción:

1. Encriptar contraseñas con BCrypt
2. Implementar Spring Security
3. Agregar validación CSRF
4. Configurar HTTPS
5. Limitar intentos de login
6. Implementar auditoría completa
7. Sanitizar inputs del usuario

---

¿Necesitas ayuda? Revisa el archivo `README_IMPLEMENTACION.md` para más detalles técnicos.
