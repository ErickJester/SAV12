package com.example.demo.service;

import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.entity.Comentario;
import com.example.demo.entity.HistorialAccion;
import com.example.demo.entity.Rol;
import com.example.demo.entity.HistorialAccion;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.ComentarioRepository;
import com.example.demo.repository.HistorialAccionRepository;
import com.example.demo.repository.HistorialAccionRepository;
import com.example.demo.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ComentarioService {

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private HistorialAccionRepository historialRepository;

    public Comentario agregarComentario(ComentarioDTO dto, Usuario usuario) {
        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        if (esStaff(usuario) && ticket.getFechaPrimeraRespuesta() == null) {
            LocalDateTime ahora = LocalDateTime.now();
            ticket.setFechaPrimeraRespuesta(ahora);
            ticket.setTiempoPrimeraRespuestaSeg(Math.toIntExact(Duration.between(ticket.getFechaCreacion(), ahora).getSeconds()));
            ticketRepository.save(ticket);
        }

        Comentario comentario = new Comentario();
        comentario.setTicket(ticket);
        comentario.setUsuario(usuario);
        comentario.setContenido(dto.getContenido());

        Comentario saved = comentarioRepository.save(comentario);

        HistorialAccion historial = new HistorialAccion();
        historial.setTicket(ticket);
        historial.setUsuario(usuario);
        historial.setTipo("COMENTARIO");
        historial.setAccion("Comentario agregado");
        historial.setDetalles(dto.getContenido());
        historialRepository.save(historial);

        return saved;
    }

    public List<Comentario> obtenerComentariosDeTicket(Ticket ticket) {
        return comentarioRepository.findByTicketOrderByFechaCreacionAsc(ticket);
    }

    private boolean esStaff(Usuario usuario) {
        return usuario.getRol() == Rol.TECNICO || usuario.getRol() == Rol.ADMIN;
    }
}
