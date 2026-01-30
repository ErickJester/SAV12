package com.example.demo.service;

import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.entity.Comentario;
import com.example.demo.entity.HistorialAccion;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.ComentarioRepository;
import com.example.demo.repository.HistorialAccionRepository;
import com.example.demo.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        Comentario savedComentario = comentarioRepository.save(comentario);

        HistorialAccion historial = new HistorialAccion();
        historial.setTicket(ticket);
        historial.setUsuario(usuario);
        historial.setTipo("COMENTARIO");
        historial.setAccion("Comentario agregado");
        historial.setDetalles(dto.getContenido());
        historialAccionRepository.save(historial);

        return savedComentario;
    }

    public List<Comentario> obtenerComentariosDeTicket(Ticket ticket) {
        return comentarioRepository.findByTicketOrderByFechaCreacionAsc(ticket);
    }
}
