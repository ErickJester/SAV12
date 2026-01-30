package com.example.demo.service;

import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.entity.Comentario;
import com.example.demo.entity.HistorialAccion;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.ComentarioRepository;
import com.example.demo.repository.HistorialAccionRepository;
import com.example.demo.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private HistorialAccionRepository historialAccionRepository;

    public Comentario agregarComentario(ComentarioDTO dto, Usuario usuario) {
        Ticket ticket = ticketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));

        Comentario comentario = new Comentario();
        comentario.setTicket(ticket);
        comentario.setUsuario(usuario);
        comentario.setContenido(dto.getContenido());

        actualizarPrimeraRespuestaSiAplica(ticket, usuario);
        registrarHistorialComentario(ticket, usuario);

        return comentarioRepository.save(comentario);
    }

    public List<Comentario> obtenerComentariosDeTicket(Ticket ticket) {
        return comentarioRepository.findByTicketOrderByFechaCreacionAsc(ticket);
    }

    private void actualizarPrimeraRespuestaSiAplica(Ticket ticket, Usuario usuario) {
        if (ticket.getFechaPrimeraRespuesta() != null) {
            return;
        }
        if (!esStaff(usuario)) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        ticket.setFechaPrimeraRespuesta(ahora);
        long segundos = Duration.between(ticket.getFechaCreacion(), ahora).getSeconds();
        ticket.setTiempoPrimeraRespuestaSeg(Math.toIntExact(segundos));
        ticketRepository.save(ticket);
    }

    private boolean esStaff(Usuario usuario) {
        Rol rol = usuario.getRol();
        return rol == Rol.TECNICO || rol == Rol.ADMIN || rol == Rol.ADMINISTRATIVO;
    }

    private void registrarHistorialComentario(Ticket ticket, Usuario usuario) {
        HistorialAccion historial = new HistorialAccion();
        historial.setTicket(ticket);
        historial.setUsuario(usuario);
        historial.setAccion("Comentario agregado");
        historial.setDetalles("COMENTARIO");
        historialAccionRepository.save(historial);
    }
}
