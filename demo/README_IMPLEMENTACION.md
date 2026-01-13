# Sistema de Administración de Tickets SAV12

## Descripción
Sistema completo de gestión de tickets con 3 roles diferenciados: Usuario, Técnico y Administrador.

## Estructura Implementada

### 1. Entidades (Entity)
- **Usuario**: Gestión de usuarios con roles (USUARIO, TECNICO, ADMIN)
- **Ticket**: Tickets de soporte con estados, prioridades y seguimiento
- **Comentario**: Comentarios en tickets para comunicación
- **Categoria**: Categorías de problemas
- **Ubicacion**: Ubicaciones físicas (edificio, piso, salón)
- **HistorialAccion**: Registro de todas las acciones en tickets
- **EstadoTicket**: Enum (ABIERTO, EN_PROCESO, RESUELTO, CERRADO, REABIERTO)
- **Prioridad**: Enum (BAJA, MEDIA, ALTA, URGENTE)
- **Rol**: Enum (USUARIO, ADMIN, TECNICO)

### 2. DTOs (Data Transfer Objects)
- TicketDTO
- ComentarioDTO
- CambioEstadoDTO
- CategoriaDTO
- UbicacionDTO
- RegistroDTO
- ReporteDTO

### 3. Repositorios (Repository)
- UsuarioRepository
- TicketRepository
- ComentarioRepository
- CategoriaRepository
- UbicacionRepository
- HistorialAccionRepository

### 4. Servicios (Service)
- **UsuarioService**: Gestión de usuarios
- **TicketService**: Crear, actualizar, cambiar estado de tickets
- **ComentarioService**: Gestión de comentarios
- **CatalogoService**: Gestión de categorías y ubicaciones
- **ReporteService**: Generación de reportes y SLA

### 5. Controladores (Controller)

#### HomeController
- `/` → Redirección a login
- `/login` (GET/POST) → Autenticación y redirección por rol
- `/logout` → Cerrar sesión
- `/registro` (GET/POST) → Registro de nuevos usuarios

#### UsuarioController (`/usuario/*`)
- `/usuario/panel` → Panel principal del usuario
- `/usuario/crear-ticket` → Formulario y creación de tickets
- `/usuario/mis-tickets` → Lista de mis tickets
- `/usuario/ticket/{id}` → Detalle de ticket con comentarios
- `/usuario/ticket/{id}/comentar` → Agregar comentario
- `/usuario/ticket/{id}/reabrir` → Reabrir ticket cerrado

#### TecnicoController (`/tecnico/*`)
- `/tecnico/panel` → Panel con tickets asignados y disponibles
- `/tecnico/mis-tickets` → Mis tickets asignados
- `/tecnico/ticket/{id}` → Detalle con opciones de cambio de estado
- `/tecnico/ticket/{id}/cambiar-estado` → Cambiar estado del ticket
- `/tecnico/ticket/{id}/comentar` → Agregar comentario técnico
- `/tecnico/ticket/{id}/asignar` → Asignarme un ticket

#### AdministradorController (`/admin/*`)
- `/admin/panel` → Panel principal con estadísticas
- `/admin/usuarios` → Gestión de usuarios y roles
- `/admin/tickets` → Todos los tickets y asignación de técnicos
- `/admin/categorias` → Gestión de categorías
- `/admin/ubicaciones` → Gestión de ubicaciones
- `/admin/reportes` → Reportes de SLA y cumplimiento
- `/admin/usuarios/{id}/cambiar-estado` → Activar/desactivar usuario
- `/admin/usuarios/{id}/cambiar-rol` → Cambiar rol de usuario
- `/admin/categorias/crear` → Crear nueva categoría
- `/admin/ubicaciones/crear` → Crear nueva ubicación
- `/admin/tickets/{id}/asignar-tecnico` → Asignar técnico a ticket

### 6. Vistas HTML (Templates)

#### Usuario
- `usuario/panel.html` → Panel principal con resumen
- `usuario/crear-ticket.html` → Formulario de creación
- `usuario/mis-tickets.html` → Tabla de tickets
- `usuario/detalle-ticket.html` → Detalle completo con comentarios

#### Técnico
- `tecnico/panel.html` → Panel con tickets disponibles y asignados
- `tecnico/mis-tickets.html` → Tickets asignados
- `tecnico/detalle-ticket.html` → Detalle con cambio de estado

#### Administrador
- `admin/panel.html` → Dashboard general
- `admin/usuarios.html` → Gestión de usuarios
- `admin/categorias.html` → Gestión de categorías
- `admin/ubicaciones.html` → Gestión de ubicaciones
- `admin/tickets.html` → Gestión de tickets
- `admin/reportes.html` → Reportes y SLA

### 7. Archivos CSS Personalizados
- `panel.css` → Estilos para paneles y navegación
- `forms.css` → Estilos para formularios
- `tickets.css` → Estilos para listas de tickets
- `ticket-detail.css` → Estilos para detalles de tickets
- `admin.css` → Estilos para vistas de administración
- `reports.css` → Estilos para reportes

## Funcionalidades por Rol

### USUARIO
✅ Crear tickets con evidencia
✅ Consultar mis tickets
✅ Ver detalles de tickets
✅ Agregar comentarios
✅ Reabrir tickets cerrados/resueltos

### TÉCNICO
✅ Ver tickets disponibles para asignar
✅ Asignarme tickets
✅ Cambiar estado de tickets
✅ Registrar historial de acciones
✅ Agregar comentarios técnicos
✅ Consultar tickets asignados

### ADMINISTRADOR
✅ Gestionar usuarios (activar/desactivar)
✅ Cambiar roles de usuarios
✅ Ver todos los tickets del sistema
✅ Asignar técnicos a tickets
✅ Gestionar catálogo de categorías
✅ Gestionar catálogo de ubicaciones
✅ Ver reportes de cumplimiento de SLA
✅ Exportar reportes
✅ Consultar resolución y tiempos de respuesta

## Características Técnicas

### Autenticación y Sesiones
- Login con validación de credenciales
- Gestión de sesiones con HttpSession
- Redirección automática según rol
- Protección de rutas por rol
- Logout con limpieza de sesión

### Base de Datos
- JPA/Hibernate para persistencia
- MySQL como motor de base de datos
- Relaciones entre entidades:
  - Usuario → Ticket (OneToMany)
  - Ticket → Comentario (OneToMany)
  - Ticket → HistorialAccion (OneToMany)
  - Ticket → Categoria (ManyToOne)
  - Ticket → Ubicacion (ManyToOne)

### Seguimiento y Auditoría
- Historial completo de acciones en cada ticket
- Registro de cambios de estado con timestamps
- Seguimiento de tiempos de respuesta para SLA
- Auditoría de quién realizó cada acción

### UI/UX
- Diseño responsive con Gumby CSS (bootstrap existente)
- Estilos personalizados sin modificar bootstrap
- Badges de colores para estados y prioridades
- Notificaciones de éxito/error
- Navegación intuitiva por rol

## Configuración de Base de Datos

```properties
spring.datasource.url=jdbc:mysql://sav12.cj4iq8uc8saj.mx-central-1.rds.amazonaws.com:3306/sav12
spring.datasource.username=admin
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=none
```

## Ejecutar el Proyecto

```bash
# Con Maven Wrapper
./mvnw spring-boot:run

# Con Maven instalado
mvn spring-boot:run
```

El servidor se ejecutará en `http://localhost:8080`

## Rutas Principales

- **Login**: http://localhost:8080/login
- **Registro**: http://localhost:8080/registro
- **Panel Usuario**: http://localhost:8080/usuario/panel
- **Panel Técnico**: http://localhost:8080/tecnico/panel
- **Panel Admin**: http://localhost:8080/admin/panel

## Próximas Mejoras Sugeridas

- [ ] Encriptación de contraseñas con BCrypt
- [ ] Paginación en listados de tickets
- [ ] Filtros y búsqueda avanzada
- [ ] Subida real de archivos de evidencia
- [ ] Notificaciones por correo
- [ ] API REST para integración móvil
- [ ] Dashboard con gráficas (Chart.js)
- [ ] Exportación real a CSV/PDF
- [ ] Sistema de permisos más granular
- [ ] Historial de cambios en usuarios

## Tecnologías Utilizadas

- Spring Boot 3.2.0
- Spring Data JPA
- Thymeleaf
- MySQL
- Gumby CSS (Bootstrap)
- jQuery
- Java 17
- Maven

---

Desarrollado siguiendo el patrón MVC y las mejores prácticas de Spring Boot.
