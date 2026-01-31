# Mapa Completo de Navegación - SAV12

## 🗺️ Estructura de Navegación por Rol

```
┌─────────────────────────────────────────────────────────────────┐
│                        LOGIN (/login)                            │
│  - Validación de credenciales                                    │
│  - Redirección automática según rol                              │
└──────────────────────┬──────────────────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
    ┌────────┐    ┌─────────┐   ┌──────────┐
    │ALUMNO │    │ TÉCNICO │   │  ADMIN   │
    └────────┘    └─────────┘   └──────────┘
```

---

## 👤 ROL: ALUMNO

### Panel Principal (`/usuario/panel`)
├── Resumen de tickets (total, abiertos, en proceso, resueltos)
├── Últimos 5 tickets creados
└── Acciones rápidas
    ├── → Crear Ticket
    └── → Ver Mis Tickets

### Crear Ticket (`/usuario/crear-ticket`)
├── Formulario de creación
│   ├── Título *
│   ├── Descripción *
│   ├── Categoría * (select)
│   ├── Ubicación * (select)
│   ├── Prioridad (BAJA, MEDIA, ALTA, URGENTE)
│   └── Evidencia (URL o descripción)
└── → POST: `/usuario/crear-ticket`
    └── Redirección: `/usuario/mis-tickets?success=created`

### Mis Tickets (`/usuario/mis-tickets`)
├── Tabla con todos mis tickets
│   ├── ID
│   ├── Título
│   ├── Estado (badge con color)
│   ├── Prioridad (badge con color)
│   ├── Fecha de creación
│   ├── Técnico asignado
│   └── [Ver] → Detalle
└── Filtros por estado (futuro)

### Detalle de Ticket (`/usuario/ticket/{id}`)
├── Información completa
│   ├── Título y estado
│   ├── Categoría, ubicación, prioridad
│   ├── Fechas (creación, actualización)
│   ├── Técnico asignado
│   └── Descripción y evidencia
├── Sección de Comentarios
│   ├── Lista de comentarios (orden cronológico)
│   └── Formulario agregar comentario
│       └── → POST: `/usuario/ticket/{id}/comentar`
├── Historial de Acciones (sidebar)
│   └── Todas las acciones con usuario y fecha
└── Opciones según estado
    └── Si está RESUELTO o CERRADO:
        └── [Reabrir Ticket] → POST: `/usuario/ticket/{id}/reabrir`

---

## 🔧 ROL: TÉCNICO

### Panel Principal (`/tecnico/panel`)
├── Resumen de tickets
│   ├── Tickets asignados a mí
│   ├── Tickets sin asignar
│   ├── En proceso
│   └── Resueltos
├── Tickets Disponibles para Asignar
│   ├── Lista de tickets sin técnico
│   └── Para cada ticket:
│       ├── [Ver Detalles]
│       └── [Asignarme] → POST: `/tecnico/ticket/{id}/asignar`
└── Mis Tickets Recientes (últimos 5)
    └── [Ver y Actualizar] → Detalle

### Mis Tickets (`/tecnico/mis-tickets`)
├── Tabla de tickets asignados a mí
│   ├── ID, Título, Usuario
│   ├── Estado, Prioridad
│   ├── Fecha de creación
│   └── [Ver/Actualizar] → Detalle
└── Ordenado por fecha (más recientes primero)

### Detalle de Ticket (`/tecnico/ticket/{id}`)
├── Información completa del ticket
│   └── (similar a usuario + más detalles)
├── **Cambiar Estado del Ticket**
│   ├── Formulario de cambio de estado
│   │   ├── Nuevo Estado (select con todos los estados)
│   │   └── Observaciones (textarea)
│   └── → POST: `/tecnico/ticket/{id}/cambiar-estado`
│       └── Registra en historial automáticamente
├── Si ticket NO asignado:
│   └── [Asignarme este Ticket]
├── Sección de Comentarios
│   ├── Ver todos los comentarios
│   └── Agregar comentario técnico
│       └── → POST: `/tecnico/ticket/{id}/comentar`
└── Historial de Acciones completo

---

## 👨‍💼 ROL: ADMINISTRADOR

### Panel Principal (`/admin/panel`)
├── Resumen General del Sistema
│   ├── Total de tickets
│   ├── Tickets por estado (6 tarjetas)
│   └── Estadísticas globales
└── Acciones Rápidas (tarjetas)
    ├── → Gestionar Usuarios
    ├── → Ver Todos los Tickets
    ├── → Gestionar Categorías
    ├── → Gestionar Ubicaciones
    └── → Ver Reportes

### Gestión de Usuarios (`/admin/usuarios`)
├── Tabla de todos los usuarios
│   ├── ID, Nombre, Correo
│   ├── Rol (badge)
│   ├── Boleta/ID Trabajador
│   ├── Estado (Activo/Inactivo)
│   └── Acciones:
│       ├── [Activar/Desactivar]
│       │   └── POST: `/admin/usuarios/{id}/cambiar-estado`
│       └── Cambiar Rol
│           ├── Select: ALUMNO, TECNICO, ADMIN
│           └── POST: `/admin/usuarios/{id}/cambiar-rol`
└── Buscar/Filtrar usuarios (futuro)

### Gestión de Categorías (`/admin/categorias`)
├── **Panel Izquierdo**: Crear Nueva Categoría
│   ├── Nombre *
│   ├── Descripción
│   └── → POST: `/admin/categorias/crear`
└── **Panel Derecho**: Categorías Existentes
    ├── Tabla con todas las categorías
    │   ├── ID, Nombre, Descripción
    │   ├── Estado (Activa/Inactiva)
    │   └── [Desactivar]
    │       └── POST: `/admin/categorias/{id}/desactivar`
    └── Solo se muestran en selects las activas

### Gestión de Ubicaciones (`/admin/ubicaciones`)
├── **Panel Izquierdo**: Crear Nueva Ubicación
│   ├── Edificio *
│   ├── Piso
│   ├── Salón
│   └── → POST: `/admin/ubicaciones/crear`
└── **Panel Derecho**: Ubicaciones Existentes
    ├── Tabla con todas las ubicaciones
    │   ├── ID, Edificio, Piso, Salón
    │   ├── Estado (Activa/Inactiva)
    │   └── [Desactivar]
    │       └── POST: `/admin/ubicaciones/{id}/desactivar`
    └── Solo se muestran en selects las activas

### Gestión de Tickets (`/admin/tickets`)
├── Tabla con TODOS los tickets del sistema
│   ├── ID, Título, Usuario
│   ├── Estado, Prioridad
│   ├── Técnico asignado
│   ├── Fecha de creación
│   └── **Asignar Técnico**
│       ├── Select con lista de técnicos
│       └── POST: `/admin/tickets/{id}/asignar-tecnico`
└── Ver estadísticas globales

### Reportes y SLA (`/admin/reportes`)
├── **Resumen General**
│   └── 6 tarjetas con totales por estado
├── **Cumplimiento de SLA**
│   ├── Porcentaje general de cumplimiento
│   ├── Tickets que cumplen SLA
│   ├── Tickets que incumplen SLA
│   └── Cálculo basado en tiempo de resolución vs SLA definido
├── **Distribución por Estado**
│   └── Tabla con cantidad de tickets por cada estado
└── **Exportar Reportes**
    ├── [Imprimir Reporte] (window.print)
    └── [Exportar a CSV] (en desarrollo)

---

## 🔄 Flujos de Trabajo

### Flujo 1: Usuario Crea y Sigue Ticket

```
Usuario → Login → Panel Usuario → Crear Ticket
    ↓
Completa formulario (título, descripción, categoría, ubicación)
    ↓
Ticket creado → Estado: ABIERTO → Historial: "Ticket creado"
    ↓
Usuario ve en "Mis Tickets"
    ↓
Puede agregar comentarios, ver estado, reabrir si se cierra
```

### Flujo 2: Técnico Gestiona Ticket

```
Técnico → Login → Panel Técnico
    ↓
Ve "Tickets Disponibles" (sin asignar)
    ↓
[Asignarme] → Ticket asignado al técnico
    ↓
Estado cambia a: EN_PROCESO
    ↓
Técnico ve detalle, analiza problema
    ↓
Agrega comentarios con solución
    ↓
Cambia estado a: RESUELTO
    ↓
Todo queda registrado en historial
```

### Flujo 3: Admin Supervisa y Configura

```
Admin → Login → Panel Admin
    ↓
Opción A: Ver reportes de SLA y cumplimiento
Opción B: Gestionar usuarios (cambiar roles, activar/desactivar)
Opción C: Crear/gestionar categorías y ubicaciones
Opción D: Asignar técnicos específicos a tickets
Opción E: Ver todos los tickets y estadísticas globales
```

---

## 🔐 Control de Acceso

### Sin Autenticación
- `/` → Redirige a `/login`
- `/login` (GET/POST)
- `/registro` (GET/POST)

### Con Autenticación - Por Rol

| Ruta                | ALUMNO | TÉCNICO | ADMIN |
|---------------------|---------|---------|-------|
| `/usuario/*`        | ✅      | ❌      | ❌    |
| `/tecnico/*`        | ❌      | ✅      | ❌    |
| `/admin/*`          | ❌      | ❌      | ✅    |
| `/logout`           | ✅      | ✅      | ✅    |

*Cada controlador verifica el rol en sesión antes de procesar la petición*

---

## 📊 Estados y Transiciones

### Estados de Ticket

```
ABIERTO → (técnico se asigna) → EN_PROCESO
    ↓                              ↓
    └────────────────────────────→ EN_ESPERA
                                    ↓
                                 RESUELTO
                                    ↓
                                 CERRADO
```

### Quién puede cambiar estados

- **ABIERTO** → **EN_PROCESO**: Técnico (al asignarse)
- **EN_PROCESO** → **EN_ESPERA**: Técnico o Admin
- **EN_ESPERA** → **EN_PROCESO**: Técnico o Admin
- **EN_PROCESO** → **RESUELTO**: Técnico
- **RESUELTO** → **CERRADO**: Técnico o Admin
- **RESUELTO/CERRADO/CANCELADO** → **ABIERTO**: Creador o Staff

---

## 📈 Métricas y Reportes

### Cálculo de SLA
```
Tiempo de Primera Respuesta = Fecha Primera Respuesta - Fecha Creación
Tiempo de Resolución Efectivo = (Fecha Resolución - Fecha Creación) - Tiempo en Espera

Si Tiempo de Primera Respuesta ≤ SLA Primera Respuesta → Cumple ✅
Si Tiempo de Resolución Efectivo ≤ SLA Resolución → Cumple ✅

Porcentaje de Cumplimiento = (Tickets que cumplen / Total resueltos) × 100
```

### Datos para Reportes
- Total de tickets en el sistema
- Tickets por estado (ABIERTO, EN_PROCESO, etc.)
- Tickets que cumplen/incumplen SLA
- Tiempo promedio de resolución
- Tickets por técnico
- Tickets por categoría

---

## 🎨 Elementos Visuales

### Badges de Estado
- **ABIERTO** → Azul (#17a2b8)
- **EN_PROCESO** → Amarillo (#ffc107)
- **EN_ESPERA** → Azul claro (#0ea5e9)
- **RESUELTO** → Verde (#28a745)
- **CERRADO** → Gris (#6c757d)
- **CANCELADO** → Naranja (#f97316)

### Badges de Prioridad
- **BAJA** → Celeste claro
- **MEDIA** → Amarillo claro
- **ALTA** → Rojo claro
- **URGENTE** → Rojo intenso

### Badges de Rol
- **ALUMNO** → Azul (#007bff)
- **DOCENTE** → Verde (#10b981)
- **ADMINISTRATIVO** → Amarillo (#f59e0b)
- **TECNICO** → Naranja (#fd7e14)
- **ADMIN** → Púrpura (#6610f2)

---

Esta es la estructura completa de navegación del sistema SAV12. Cada ruta está protegida por validación de sesión y rol.
