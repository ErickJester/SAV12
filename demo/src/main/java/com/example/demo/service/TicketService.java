package com.example.demo.service;

import com.example.demo.DTO.TicketDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private UbicacionRepository ubicacionRepository;

    @Autowired
    private HistorialAccionRepository historialRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SlaPoliticaRepository slaPoliticaRepository;

    public Ticket crearTicket(TicketDTO dto, Usuario usuario) {
        Ticket ticket = new Ticket();
        ticket.setTitulo(dto.getTitulo());
        ticket.setDescripcion(dto.getDescripcion());
        ticket.setCreadoPor(usuario);
        ticket.setEstado(EstadoTicket.ABIERTO);
        ticket.setEvidenciaProblema(dto.getEvidenciaProblema());
        ticket.setSlaPolitica(obtenerSlaPorRol(usuario));

        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getCategoriaId()).orElse(null);
            ticket.setCategoria(categoria);
        }

        if (dto.getUbicacionId() != null) {
            Ubicacion ubicacion = ubicacionRepository.findById(dto.getUbicacionId()).orElse(null);
            ticket.setUbicacion(ubicacion);
        }

        if (dto.getPrioridad() != null && !dto.getPrioridad().isEmpty()) {
            ticket.setPrioridad(Prioridad.valueOf(dto.getPrioridad()));
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        // Registrar en historial
        registrarHistorial(savedTicket, usuario, "CREACION", null, EstadoTicket.ABIERTO, "Ticket creado");

        // Notificar a técnicos sobre nuevo reporte
        emailService.notifyTechniciansOnNewTicket(savedTicket);

        return savedTicket;
    }

    public Ticket cambiarEstado(Long ticketId, EstadoTicket nuevoEstado, Usuario usuario, String observaciones) {
        return cambiarEstado(ticketId, nuevoEstado, usuario, observaciones, null);
    }

    public Ticket cambiarEstado(Long ticketId, EstadoTicket nuevoEstado, Usuario usuario, String observaciones, String evidenciaResolucion) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        LocalDateTime ahora = LocalDateTime.now();
        EstadoTicket estadoAnterior = ticket.getEstado();
        ticket.setEstado(nuevoEstado);
        ticket.setFechaActualizacion(ahora);

        if (estadoAnterior == EstadoTicket.EN_ESPERA && nuevoEstado != EstadoTicket.EN_ESPERA) {
            actualizarTiempoEspera(ticket, ahora);
        }

        if (nuevoEstado == EstadoTicket.EN_ESPERA) {
            ticket.setEsperaDesde(ahora);
        }

        marcarPrimeraRespuestaSiCorresponde(ticket, usuario, ahora);

        if (nuevoEstado == EstadoTicket.RESUELTO) {
            ticket.setFechaResolucion(ahora);
            establecerTiempoResolucionSiCorresponde(ticket, ahora);
        }

        if (nuevoEstado == EstadoTicket.CERRADO) {
            ticket.setFechaCierre(ahora);
            if (ticket.getFechaResolucion() == null) {
                ticket.setFechaResolucion(ahora);
            }
            establecerTiempoResolucionSiCorresponde(ticket, ahora);
        }

        if (evidenciaResolucion != null && !evidenciaResolucion.isEmpty()) {
            ticket.setEvidenciaResolucion(evidenciaResolucion);
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        // Registrar en historial
        String detalle = "Estado cambiado de " + estadoAnterior + " a " + nuevoEstado;
        if (observaciones != null && !observaciones.isEmpty()) {
            detalle = detalle + " - " + observaciones;
        }
        registrarHistorial(savedTicket, usuario, "ESTADO", estadoAnterior, nuevoEstado, detalle);

        // Notificar al usuario sobre cambio de estado
        emailService.notifyUserOnTicketChange(savedTicket, detalle);

        return savedTicket;
    }

    public Ticket asignarTecnico(Long ticketId, Usuario tecnico, Usuario actor) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        LocalDateTime ahora = LocalDateTime.now();
        Usuario asignadoAnterior = ticket.getAsignadoA();
        ticket.setAsignadoA(tecnico);
        ticket.setFechaActualizacion(ahora);
        marcarPrimeraRespuestaSiCorresponde(ticket, actor, ahora);
        
        Ticket savedTicket = ticketRepository.save(ticket);
        String anterior = asignadoAnterior != null ? asignadoAnterior.getNombre() : "Sin asignar";
        String nuevo = tecnico != null ? tecnico.getNombre() : "Sin asignar";
        String detalles = "Asignado de " + anterior + " a " + nuevo;
        registrarHistorial(savedTicket, actor, "ASIGNACION", null, null, detalles);
        // Notificar al técnico asignado
        emailService.notifyAssignedTechnician(savedTicket);
        
        return savedTicket;
    }

    public Ticket reabrirTicket(Long ticketId, Usuario usuario) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (!esCreadorOStaff(ticket, usuario)) {
            throw new RuntimeException("No autorizado para reabrir el ticket");
        }

        if (ticket.getEstado() == EstadoTicket.RESUELTO
                || ticket.getEstado() == EstadoTicket.CERRADO
                || ticket.getEstado() == EstadoTicket.CANCELADO) {
            EstadoTicket estadoAnterior = ticket.getEstado();
            ticket.setEstado(EstadoTicket.ABIERTO);
            ticket.setFechaActualizacion(LocalDateTime.now());
            ticket.setReabiertoCount(Optional.ofNullable(ticket.getReabiertoCount()).orElse(0) + 1);

            Ticket savedTicket = ticketRepository.save(ticket);
            String detalle = "Ticket reabierto por " + (usuario != null ? usuario.getNombre() : "usuario");
            registrarHistorial(savedTicket, usuario, "REAPERTURA", estadoAnterior, EstadoTicket.ABIERTO, detalle);
            return savedTicket;
        }

        return ticket;
    }

    public Ticket reabrirTicketComoAdmin(Long ticketId, Usuario admin) {
        return reabrirTicket(ticketId, admin);
    }

    public List<Ticket> obtenerTicketsDeUsuario(Usuario usuario) {
        return ticketRepository.findByCreadoPor(usuario)
                .stream()
                .sorted((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()))
                .collect(Collectors.toList());
    }

    public List<Ticket> obtenerTicketsDeTecnico(Usuario tecnico) {
        return ticketRepository.findByAsignadoA(tecnico)
                .stream()
                .sorted((a, b) -> b.getFechaCreacion().compareTo(a.getFechaCreacion()))
                .collect(Collectors.toList());
    }

    public List<Ticket> obtenerTodosLosTickets() {
        return ticketRepository.findAllByOrderByFechaCreacionDesc();
    }

    public Ticket obtenerTicketPorId(Long id) {
        return ticketRepository.findById(id).orElse(null);
    }

    private void registrarHistorial(Ticket ticket, Usuario usuario, String accion, 
                                     EstadoTicket estadoAnterior, EstadoTicket estadoNuevo, String detalles) {
        HistorialAccion historial = new HistorialAccion();
        historial.setTicket(ticket);
        historial.setUsuario(usuario);
        historial.setAccion(accion);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setDetalles(detalles);
        historialRepository.save(historial);
    }

    public List<HistorialAccion> obtenerHistorialDeTicket(Ticket ticket) {
        return historialRepository.findByTicketOrderByFechaAccionDesc(ticket);
    }

    private boolean esStaff(Usuario usuario) {
        if (usuario == null || usuario.getRol() == null) {
            return false;
        }
        return usuario.getRol() == Rol.TECNICO
                || usuario.getRol() == Rol.ADMIN
                || usuario.getRol() == Rol.ADMINISTRATIVO;
    }

    private boolean esCreadorOStaff(Ticket ticket, Usuario usuario) {
        return ticket.getCreadoPor() != null
                && usuario != null
                && (ticket.getCreadoPor().getId().equals(usuario.getId()) || esStaff(usuario));
    }

    private void marcarPrimeraRespuestaSiCorresponde(Ticket ticket, Usuario actor, LocalDateTime ahora) {
        if (ticket.getFechaPrimeraRespuesta() == null && esStaff(actor)) {
            ticket.setFechaPrimeraRespuesta(ahora);
            long segundos = Duration.between(ticket.getFechaCreacion(), ahora).getSeconds();
            ticket.setTiempoPrimeraRespuestaSeg((int) Math.max(segundos, 0));
        }
    }

    private void establecerTiempoResolucionSiCorresponde(Ticket ticket, LocalDateTime ahora) {
        if (ticket.getTiempoResolucionSeg() == null) {
            long segundos = Duration.between(ticket.getFechaCreacion(), ahora).getSeconds();
            ticket.setTiempoResolucionSeg((int) Math.max(segundos, 0));
        }
    }

    private void actualizarTiempoEspera(Ticket ticket, LocalDateTime ahora) {
        if (ticket.getEsperaDesde() != null) {
            long segundos = Duration.between(ticket.getEsperaDesde(), ahora).getSeconds();
            int acumulado = Optional.ofNullable(ticket.getTiempoEsperaSeg()).orElse(0);
            ticket.setTiempoEsperaSeg((int) (acumulado + Math.max(segundos, 0)));
            ticket.setEsperaDesde(null);
        }
    }

    private SlaPolitica obtenerSlaPorRol(Usuario usuario) {
        String nombreRol = "ALUMNO";
        if (usuario != null && usuario.getRol() != null) {
            Rol rol = usuario.getRol();
            if (rol == Rol.DOCENTE || rol == Rol.ADMINISTRATIVO || rol == Rol.ALUMNO) {
                nombreRol = rol.name();
            }
        }
        return slaPoliticaRepository.findByNombre(nombreRol).orElseGet(() ->
                slaPoliticaRepository.findByNombre("ALUMNO").orElse(null));
    }
}
