package com.example.demo.controller;

import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.entity.*;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/tecnico")
public class TecnicoController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ComentarioService comentarioService;

    @Autowired
    private UsuarioService usuarioService;

    // Panel principal del técnico
    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        Usuario tecnico = (Usuario) session.getAttribute("usuario");
        if (tecnico == null || tecnico.getRol() != Rol.TECNICO) {
            return "redirect:/login";
        }

        List<Ticket> misTickets = ticketService.obtenerTicketsDeTecnico(tecnico).stream()
                .sorted(Comparator.comparing(Ticket::getFechaCreacion).reversed())
                .collect(Collectors.toList());
        List<Ticket> ticketsSinAsignar = ticketService.obtenerTodosLosTickets()
                .stream()
                .filter(t -> t.getTecnico() == null && t.getEstado() == EstadoTicket.ABIERTO)
                .sorted(Comparator.comparing(Ticket::getFechaCreacion).reversed())
                .collect(Collectors.toList());

        model.addAttribute("usuario", tecnico);
        model.addAttribute("misTickets", misTickets);
        model.addAttribute("ticketsSinAsignar", ticketsSinAsignar);
        return "tecnico/panel";
    }

    // Ver mis tickets asignados
    @GetMapping("/mis-tickets")
    public String misTickets(HttpSession session, Model model) {
        Usuario tecnico = (Usuario) session.getAttribute("usuario");
        if (tecnico == null || tecnico.getRol() != Rol.TECNICO) {
            return "redirect:/login";
        }

        List<Ticket> tickets = ticketService.obtenerTicketsDeTecnico(tecnico);
        model.addAttribute("tickets", tickets);
        model.addAttribute("usuario", tecnico);
        return "tecnico/mis-tickets";
    }

    // Ver detalle de ticket
    @GetMapping("/ticket/{id}")
    public String verDetalleTicket(@PathVariable Long id, HttpSession session, Model model) {
        Usuario tecnico = (Usuario) session.getAttribute("usuario");
        if (tecnico == null || tecnico.getRol() != Rol.TECNICO) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket == null) {
            return "redirect:/tecnico/mis-tickets?error=notfound";
        }

        List<Comentario> comentarios = comentarioService.obtenerComentariosDeTicket(ticket);
        List<HistorialAccion> historial = ticketService.obtenerHistorialDeTicket(ticket);

        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);
        model.addAttribute("historial", historial);
        model.addAttribute("usuario", tecnico);
        model.addAttribute("estados", EstadoTicket.values());
        return "tecnico/detalle-ticket";
    }

    // Cambiar estado del ticket
    @PostMapping("/ticket/{id}/cambiar-estado")
    public String cambiarEstado(@PathVariable Long id,
                                 @RequestParam String nuevoEstado,
                                 @RequestParam(required = false) String observaciones,
                                 HttpSession session) {
        Usuario tecnico = (Usuario) session.getAttribute("usuario");
        if (tecnico == null || tecnico.getRol() != Rol.TECNICO) {
            return "redirect:/login";
        }

        try {
            EstadoTicket estado = EstadoTicket.valueOf(nuevoEstado);
            ticketService.cambiarEstado(id, estado, tecnico, observaciones);
        } catch (Exception e) {
            return "redirect:/tecnico/ticket/" + id + "?error=cambioestado";
        }

        return "redirect:/tecnico/ticket/" + id + "?success=estadocambiado";
    }

    // Agregar comentario
    @PostMapping("/ticket/{id}/comentar")
    public String agregarComentario(@PathVariable Long id,
                                     @RequestParam String contenido,
                                     HttpSession session) {
        Usuario tecnico = (Usuario) session.getAttribute("usuario");
        if (tecnico == null || tecnico.getRol() != Rol.TECNICO) {
            return "redirect:/login";
        }

        ComentarioDTO dto = new ComentarioDTO();
        dto.setTicketId(id);
        dto.setContenido(contenido);

        comentarioService.agregarComentario(dto, tecnico);
        return "redirect:/tecnico/ticket/" + id;
    }

    // Asignarme un ticket
    @PostMapping("/ticket/{id}/asignar")
    public String asignarmeTicket(@PathVariable Long id, HttpSession session) {
        Usuario tecnico = (Usuario) session.getAttribute("usuario");
        if (tecnico == null || tecnico.getRol() != Rol.TECNICO) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket != null && ticket.getTecnico() == null) {
            ticketService.asignarTecnico(id, tecnico);
            ticketService.cambiarEstado(id, EstadoTicket.EN_PROCESO, tecnico, "Ticket asignado y tomado en proceso");
        }

        return "redirect:/tecnico/ticket/" + id;
    }
}
