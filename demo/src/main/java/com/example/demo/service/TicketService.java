package com.example.demo.service;

import com.example.demo.DTO.TicketDTO;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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

        Ticket savedTicket = ticketRepository.save(ticket);

        // Registrar en historial
        registrarHistorial(savedTicket, usuario, "Ticket creado", null, EstadoTicket.ABIERTO, null);

        // Notificar a técnicos sobre nuevo reporte
        emailService.notifyTechniciansOnNewTicket(savedTicket);

        return savedTicket;
    }

    public Ticket cambiarEstado(Long ticketId, EstadoTicket nuevoEstado, Usuario usuario, String observaciones) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        EstadoTicket estadoAnterior = ticket.getEstado();
        ticket.setEstado(nuevoEstado);
        ticket.setFechaActualizacion(LocalDateTime.now());

        if (nuevoEstado == EstadoTicket.RESUELTO || nuevoEstado == EstadoTicket.CERRADO) {
            ticket.setFechaResolucion(LocalDateTime.now());
        }

        Ticket savedTicket = ticketRepository.save(ticket);

        // Registrar en historial
        String accion = "Estado cambiado de " + estadoAnterior + " a " + nuevoEstado;
        registrarHistorial(savedTicket, usuario, accion, estadoAnterior, nuevoEstado, observaciones);

        // Notificar al usuario sobre cambio de estado
        emailService.notifyUserOnTicketChange(savedTicket, accion + (observaciones != null ? " - " + observaciones : ""));

        return savedTicket;
    }

    public Ticket asignarTecnico(Long ticketId, Usuario tecnico) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        ticket.setAsignadoA(tecnico);
        ticket.setFechaActualizacion(LocalDateTime.now());
        
        Ticket savedTicket = ticketRepository.save(ticket);
        registrarHistorial(savedTicket, tecnico, "Técnico asignado: " + tecnico.getNombre(), null, null, null);
        // Notificar al técnico asignado
        emailService.notifyAssignedTechnician(savedTicket);
        
        return savedTicket;
    }

    public Ticket reabrirTicket(Long ticketId, Usuario usuario) {
        return cambiarEstado(ticketId, EstadoTicket.REABIERTO, usuario, "Ticket reabierto por el usuario");
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
}
