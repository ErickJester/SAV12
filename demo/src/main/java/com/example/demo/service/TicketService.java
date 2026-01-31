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
    private SlaPoliticaRepository slaPoliticaRepository;

    @Autowired
    private EmailService emailService;

    public Ticket crearTicket(TicketDTO dto, Usuario usuario) {
        Ticket ticket = new Ticket();
        ticket.setTitulo(dto.getTitulo());
        ticket.setDescripcion(dto.getDescripcion());
        ticket.setCreadoPor(usuario);
        ticket.setEstado(EstadoTicket.ABIERTO);
        ticket.setEvidenciaProblema(dto.getEvidenciaProblema());

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

        ticket.setSlaPolitica(obtenerSlaParaRol(usuario.getRol()));

        Ticket savedTicket = ticketRepository.save(ticket);

        registrarHistorial(savedTicket, usuario, "CREACION", "Ticket creado", null, EstadoTicket.ABIERTO, null, null, null);

        emailService.notifyTechniciansOnNewTicket(savedTicket);

        return savedTicket;
    }

    public Ticket cambiarEstado(Long ticketId, EstadoTicket nuevoEstado, Usuario usuario, String observaciones,
                                String evidenciaResolucion) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        EstadoTicket estadoAnterior = ticket.getEstado();
        LocalDateTime ahora = LocalDateTime.now();

        if (nuevoEstado == EstadoTicket.EN_ESPERA && ticket.getEsperaDesde() == null) {
            ticket.setEsperaDesde(ahora);
        }

        if (estadoAnterior == EstadoTicket.EN_ESPERA && nuevoEstado != EstadoTicket.EN_ESPERA) {
            if (ticket.getEsperaDesde() != null) {
                long esperaSeg = Duration.between(ticket.getEsperaDesde(), ahora).getSeconds();
                int acumulado = ticket.getTiempoEsperaSeg() != null ? ticket.getTiempoEsperaSeg() : 0;
                ticket.setTiempoEsperaSeg(Math.toIntExact(acumulado + esperaSeg));
                ticket.setEsperaDesde(null);
            }
        }

        ticket.setEstado(nuevoEstado);
        ticket.setFechaActualizacion(ahora);

        if (ticket.getFechaPrimeraRespuesta() == null && esStaff(usuario)) {
            ticket.setFechaPrimeraRespuesta(ahora);
            ticket.setTiempoPrimeraRespuestaSeg(Math.toIntExact(Duration.between(ticket.getFechaCreacion(), ahora).getSeconds()));
        }

        if (nuevoEstado == EstadoTicket.RESUELTO) {
            if (ticket.getFechaResolucion() == null) {
                ticket.setFechaResolucion(ahora);
            }
            LocalDateTime fechaResolucion = ticket.getFechaResolucion();
            ticket.setTiempoResolucionSeg(Math.toIntExact(Duration.between(ticket.getFechaCreacion(), fechaResolucion).getSeconds()));
            if (evidenciaResolucion != null) {
                ticket.setEvidenciaResolucion(evidenciaResolucion);
            }
        }

        if (nuevoEstado == EstadoTicket.CERRADO) {
            ticket.setFechaCierre(ahora);
            if (ticket.getFechaResolucion() == null) {
                ticket.setFechaResolucion(ahora);
            }
            LocalDateTime fechaResolucion = ticket.getFechaResolucion();
            ticket.setTiempoResolucionSeg(Math.toIntExact(Duration.between(ticket.getFechaCreacion(), fechaResolucion).getSeconds()));
            if (evidenciaResolucion != null) {
                ticket.setEvidenciaResolucion(evidenciaResolucion);
            }
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        String accion = "Estado cambiado de " + estadoAnterior + " a " + nuevoEstado;
        registrarHistorial(savedTicket, usuario, "ESTADO", accion, estadoAnterior, nuevoEstado, observaciones, null, null);

        emailService.notifyUserOnTicketChange(savedTicket, accion + (observaciones != null ? " - " + observaciones : ""));

        return savedTicket;
    }

    public Ticket asignarTecnico(Long ticketId, Usuario asignadoNuevo, Usuario actor) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Usuario asignadoAnterior = ticket.getAsignadoA();
        ticket.setAsignadoA(asignadoNuevo);
        LocalDateTime ahora = LocalDateTime.now();
        ticket.setFechaActualizacion(ahora);

        if (ticket.getFechaPrimeraRespuesta() == null && esStaff(actor)) {
            ticket.setFechaPrimeraRespuesta(ahora);
            ticket.setTiempoPrimeraRespuestaSeg(Math.toIntExact(Duration.between(ticket.getFechaCreacion(), ahora).getSeconds()));
        }

        Ticket savedTicket = ticketRepository.save(ticket);
        registrarHistorial(savedTicket, actor, "ASIGNACION", "Asignación de ticket", null, null, null,
                asignadoAnterior, asignadoNuevo);
        emailService.notifyAssignedTechnician(savedTicket);

        return savedTicket;
    }

    public Ticket reabrirTicket(Long ticketId, Usuario usuario) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        boolean esCreador = ticket.getCreadoPor() != null && ticket.getCreadoPor().getId().equals(usuario.getId());
        if (!esCreador && !esStaff(usuario)) {
            throw new RuntimeException("No autorizado para reabrir el ticket");
        }

        if (ticket.getEstado() == EstadoTicket.RESUELTO
                || ticket.getEstado() == EstadoTicket.CERRADO
                || ticket.getEstado() == EstadoTicket.CANCELADO) {
            EstadoTicket estadoAnterior = ticket.getEstado();
            ticket.setEstado(EstadoTicket.REABIERTO);
            ticket.setFechaActualizacion(LocalDateTime.now());
            int reabiertos = ticket.getReabiertoCount() != null ? ticket.getReabiertoCount() : 0;
            ticket.setReabiertoCount(reabiertos + 1);
            Ticket savedTicket = ticketRepository.save(ticket);
            registrarHistorial(savedTicket, usuario, "REAPERTURA", "Ticket reabierto", estadoAnterior,
                    EstadoTicket.REABIERTO, null, null, null);
            return savedTicket;
        }

        return ticket;
    }

    public List<Ticket> obtenerTicketsDeUsuario(Usuario usuario) {
        return ticketRepository.findByCreadoPorOrderByFechaCreacionDesc(usuario);
    }

    public List<Ticket> obtenerTicketsDeTecnico(Usuario tecnico) {
        return ticketRepository.findByAsignadoAOrderByFechaCreacionDesc(tecnico);
    }

    public List<Ticket> obtenerTodosLosTickets() {
        return ticketRepository.findAllByOrderByFechaCreacionDesc();
    }

    public Ticket obtenerTicketPorId(Long id) {
        return ticketRepository.findById(id).orElse(null);
    }

    public List<HistorialAccion> obtenerHistorialDeTicket(Ticket ticket) {
        return historialRepository.findByTicketOrderByFechaAccionDesc(ticket);
    }

    private void registrarHistorial(Ticket ticket, Usuario usuario, String tipo, String accion,
                                    EstadoTicket estadoAnterior, EstadoTicket estadoNuevo, String detalles,
                                    Usuario asignadoAnterior, Usuario asignadoNuevo) {
        HistorialAccion historial = new HistorialAccion();
        historial.setTicket(ticket);
        historial.setUsuario(usuario);
        historial.setTipo(tipo);
        historial.setAccion(accion);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setDetalles(detalles);
        historial.setAsignadoAnterior(asignadoAnterior);
        historial.setAsignadoNuevo(asignadoNuevo);
        historialRepository.save(historial);
    }

    private boolean esStaff(Usuario usuario) {
        return usuario.getRol() == Rol.TECNICO || usuario.getRol() == Rol.ADMIN;
    }

    private SlaPolitica obtenerSlaParaRol(Rol rol) {
        String rolSolicitante = (rol == Rol.ALUMNO || rol == Rol.DOCENTE || rol == Rol.ADMINISTRATIVO)
                ? rol.name()
                : Rol.ALUMNO.name();
        Optional<SlaPolitica> sla = slaPoliticaRepository.findFirstByRolSolicitanteAndActivoTrue(rolSolicitante);
        return sla.orElseGet(() ->
                slaPoliticaRepository.findFirstByRolSolicitanteAndActivoTrue(Rol.ALUMNO.name())
                        .orElseThrow(() -> new RuntimeException("No hay política SLA activa")));
    }
}
