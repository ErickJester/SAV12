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
    private SlaPoliticaRepository slaPoliticaRepository;

    @Autowired
    private EmailService emailService;

    public Ticket crearTicket(TicketDTO dto, Usuario usuario) {
        Ticket ticket = new Ticket();
        ticket.setTitulo(dto.getTitulo());
        ticket.setDescripcion(dto.getDescripcion());
        ticket.setCreadoPor(usuario);

        // Fechas base (evita NPE al calcular duraciones si tu entidad no las setea sola)
        if (ticket.getFechaCreacion() == null) {
            ticket.setFechaCreacion(LocalDateTime.now());
        }
        ticket.setFechaActualizacion(ticket.getFechaCreacion());

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

        registrarHistorial(
                savedTicket,
                usuario,
                "CREACION",
                "Ticket creado",
                null,
                EstadoTicket.ABIERTO,
                null,
                null,
                null
        );

        emailService.notifyTechniciansOnNewTicket(savedTicket);
        return savedTicket;
    }

    public Ticket cambiarEstado(Long ticketId,
                               EstadoTicket nuevoEstado,
                               Usuario usuario,
                               String observaciones,
                               String evidenciaResolucion) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        EstadoTicket estadoAnterior = ticket.getEstado();
        LocalDateTime ahora = LocalDateTime.now();

        // REABIERTO: debe pasar por aquí para no desincronizar contadores/fechas
        if (nuevoEstado == EstadoTicket.REABIERTO) {
            boolean esCreador = ticket.getCreadoPor() != null
                    && ticket.getCreadoPor().getId().equals(usuario.getId());

            if (!esCreador && !esStaff(usuario)) {
                throw new RuntimeException("No autorizado para reabrir el ticket");
            }

            if (!(estadoAnterior == EstadoTicket.RESUELTO
                    || estadoAnterior == EstadoTicket.CERRADO
                    || estadoAnterior == EstadoTicket.CANCELADO)) {
                throw new RuntimeException("Solo se puede reabrir un ticket RESUELTO/CERRADO/CANCELADO");
            }

            int reabiertos = ticket.getReabiertoCount() != null ? ticket.getReabiertoCount() : 0;
            ticket.setReabiertoCount(reabiertos + 1);

            // Reset del ciclo anterior: evita valores viejos
            ticket.setFechaResolucion(null);
            ticket.setTiempoResolucionSeg(null);
            ticket.setFechaCierre(null);
            ticket.setEvidenciaResolucion(null);
        }

        // EN_ESPERA: arranca timer
        if (nuevoEstado == EstadoTicket.EN_ESPERA && ticket.getEsperaDesde() == null) {
            ticket.setEsperaDesde(ahora);
        }

        // Salida de EN_ESPERA: acumula
        if (estadoAnterior == EstadoTicket.EN_ESPERA && nuevoEstado != EstadoTicket.EN_ESPERA) {
            if (ticket.getEsperaDesde() != null) {
                long esperaSeg = Duration.between(ticket.getEsperaDesde(), ahora).getSeconds();
                int acumulado = ticket.getTiempoEsperaSeg() != null ? ticket.getTiempoEsperaSeg() : 0;
                long total = (long) acumulado + esperaSeg;
                ticket.setTiempoEsperaSeg(total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total);
                ticket.setEsperaDesde(null);
            }
        }

        ticket.setEstado(nuevoEstado);
        ticket.setFechaActualizacion(ahora);

        // Primera respuesta (solo staff)
        if (ticket.getFechaPrimeraRespuesta() == null && esStaff(usuario)) {
            ticket.setFechaPrimeraRespuesta(ahora);
            long seg = Duration.between(ticket.getFechaCreacion(), ahora).getSeconds();
            ticket.setTiempoPrimeraRespuestaSeg(seg > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seg);
        }

        // RESUELTO: setea resolución y tiempo
        if (nuevoEstado == EstadoTicket.RESUELTO) {
            ticket.setFechaResolucion(ahora); // en RESUELTO siempre corresponde a "este" ciclo
            long seg = Duration.between(ticket.getFechaCreacion(), ticket.getFechaResolucion()).getSeconds();
            ticket.setTiempoResolucionSeg(seg > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seg);

            if (evidenciaResolucion != null && !evidenciaResolucion.isBlank()) {
                ticket.setEvidenciaResolucion(evidenciaResolucion);
            }
        }

        // CERRADO: setea cierre + si no hay resolución, la pone
        if (nuevoEstado == EstadoTicket.CERRADO) {
            ticket.setFechaCierre(ahora);
            if (ticket.getFechaResolucion() == null) {
                ticket.setFechaResolucion(ahora);
            }
            long seg = Duration.between(ticket.getFechaCreacion(), ticket.getFechaResolucion()).getSeconds();
            ticket.setTiempoResolucionSeg(seg > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seg);

            if (evidenciaResolucion != null && !evidenciaResolucion.isBlank()) {
                ticket.setEvidenciaResolucion(evidenciaResolucion);
            }
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        String tipo = (nuevoEstado == EstadoTicket.REABIERTO) ? "REAPERTURA" : "ESTADO";
        String accion = (nuevoEstado == EstadoTicket.REABIERTO)
                ? "Ticket reabierto"
                : "Estado cambiado de " + estadoAnterior + " a " + nuevoEstado;

        registrarHistorial(
                savedTicket,
                usuario,
                tipo,
                accion,
                estadoAnterior,
                nuevoEstado,
                observaciones,
                null,
                null
        );

        emailService.notifyUserOnTicketChange(
                savedTicket,
                accion + (observaciones != null ? " - " + observaciones : "")
        );

        // En reapertura, avisa a asignado o a técnicos
        if (nuevoEstado == EstadoTicket.REABIERTO) {
            if (savedTicket.getAsignadoA() != null) {
                emailService.notifyAssignedTechnician(savedTicket);
            } else {
                emailService.notifyTechniciansOnNewTicket(savedTicket);
            }
        }

        return savedTicket;
    }

    public Ticket asignarTecnico(Long ticketId, Usuario asignadoNuevo, Usuario actor) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Usuario asignadoAnterior = ticket.getAsignadoA();
        ticket.setAsignadoA(asignadoNuevo);

        LocalDateTime ahora = LocalDateTime.now();
        ticket.setFechaActualizacion(ahora);

        // Primera respuesta al asignar (si el actor es staff)
        if (ticket.getFechaPrimeraRespuesta() == null && esStaff(actor)) {
            ticket.setFechaPrimeraRespuesta(ahora);
            long seg = Duration.between(ticket.getFechaCreacion(), ahora).getSeconds();
            ticket.setTiempoPrimeraRespuestaSeg(seg > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seg);
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        registrarHistorial(
                savedTicket,
                actor,
                "ASIGNACION",
                "Asignación de ticket",
                null,
                null,
                null,
                asignadoAnterior,
                asignadoNuevo
        );

        emailService.notifyAssignedTechnician(savedTicket);
        return savedTicket;
    }

    public Ticket reabrirTicket(Long ticketId, Usuario usuario) {
        return cambiarEstado(ticketId, EstadoTicket.REABIERTO, usuario, "Ticket reabierto", null);
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

    public List<HistorialAccion> obtenerHistorialDeTicket(Ticket ticket) {
        return historialRepository.findByTicketOrderByFechaAccionDesc(ticket);
    }

    private void registrarHistorial(Ticket ticket,
                                    Usuario usuario,
                                    String tipo,
                                    String accion,
                                    EstadoTicket estadoAnterior,
                                    EstadoTicket estadoNuevo,
                                    String detalles,
                                    Usuario asignadoAnterior,
                                    Usuario asignadoNuevo) {

        HistorialAccion historial = new HistorialAccion();
        historial.setTicket(ticket);
        historial.setUsuario(usuario);
        historial.setTipo(tipo);
        historial.setAccion(accion);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setAsignadoAnterior(asignadoAnterior);
        historial.setAsignadoNuevo(asignadoNuevo);
        historial.setDetalles(detalles);

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
