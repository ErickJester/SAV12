package com.example.demo.controller;

import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.entity.Comentario;
import com.example.demo.entity.EstadoTicket;
import com.example.demo.entity.HistorialAccion;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.service.ComentarioService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.TicketService;
import com.example.demo.service.UsuarioService;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/tecnico")
public class TecnicoController {

    private final TicketService ticketService;
    private final ComentarioService comentarioService;
    private final UsuarioService usuarioService;
    private final FileStorageService fileStorageService;

    public TecnicoController(
            TicketService ticketService,
            ComentarioService comentarioService,
            UsuarioService usuarioService,
            FileStorageService fileStorageService
    ) {
        this.ticketService = ticketService;
        this.comentarioService = comentarioService;
        this.usuarioService = usuarioService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public String index() {
        return "redirect:/tecnico/panel";
    }

    @GetMapping("/panel")
    public String panel(java.security.Principal principal, Model model) {
        Usuario tecnico = usuarioService.obtenerPorCorreo(principal.getName());

        List<Ticket> misTickets = ticketService.obtenerTicketsDeTecnico(tecnico).stream()
                .sorted(Comparator.comparing(Ticket::getFechaCreacion).reversed())
                .collect(Collectors.toList());
        List<Ticket> ticketsSinAsignar = ticketService.obtenerTodosLosTickets().stream()
                .filter(t -> t.getAsignadoA() == null
                        && (t.getEstado() == EstadoTicket.ABIERTO || t.getEstado() == EstadoTicket.REABIERTO))
                .sorted(Comparator.comparing(Ticket::getFechaCreacion).reversed())
                .collect(Collectors.toList());

        model.addAttribute("usuario", tecnico);
        model.addAttribute("misTickets", misTickets);
        model.addAttribute("ticketsSinAsignar", ticketsSinAsignar);
        return "tecnico/panel";
    }

    @GetMapping("/mis-tickets")
    public String misTickets(java.security.Principal principal, Model model) {
        Usuario tecnico = usuarioService.obtenerPorCorreo(principal.getName());
        List<Ticket> tickets = ticketService.obtenerTicketsDeTecnico(tecnico);
        model.addAttribute("tickets", tickets);
        model.addAttribute("usuario", tecnico);
        return "tecnico/mis-tickets";
    }

    @GetMapping("/ticket/{id}")
    public String verDetalleTicket(@PathVariable Long id, java.security.Principal principal, Model model) {
        Usuario tecnico = usuarioService.obtenerPorCorreo(principal.getName());
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

    @PostMapping("/ticket/{id}/cambiar-estado")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam String nuevoEstado,
            @RequestParam(required = false) String observaciones,
            @RequestParam(value = "evidenciaResolucion", required = false) MultipartFile evidenciaResolucion,
            java.security.Principal principal
    ) {
        Usuario tecnico = usuarioService.obtenerPorCorreo(principal.getName());

        try {
            EstadoTicket estado = EstadoTicket.valueOf(nuevoEstado);
            String evidenciaFilename = null;
            if (evidenciaResolucion != null && !evidenciaResolucion.isEmpty()) {
                evidenciaFilename = fileStorageService.guardarArchivo(evidenciaResolucion);
            }
            ticketService.cambiarEstado(id, estado, tecnico, observaciones, evidenciaFilename);
        } catch (Exception e) {
            return "redirect:/tecnico/ticket/" + id + "?error=cambioestado";
        }

        return "redirect:/tecnico/ticket/" + id + "?success=estadocambiado";
    }

    @PostMapping("/ticket/{id}/comentar")
    public String agregarComentario(@PathVariable Long id, @RequestParam String contenido, java.security.Principal principal) {
        Usuario tecnico = usuarioService.obtenerPorCorreo(principal.getName());

        ComentarioDTO dto = new ComentarioDTO();
        dto.setTicketId(id);
        dto.setContenido(contenido);

        comentarioService.agregarComentario(dto, tecnico);
        return "redirect:/tecnico/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/reabrir")
    public String reabrirTicket(@PathVariable Long id, java.security.Principal principal) {
        Usuario tecnico = usuarioService.obtenerPorCorreo(principal.getName());

        try {
            ticketService.reabrirTicket(id, tecnico);
        } catch (Exception e) {
            return "redirect:/tecnico/ticket/" + id + "?error=reabrir";
        }
        return "redirect:/tecnico/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/asignar")
    public String asignarmeTicket(@PathVariable Long id, java.security.Principal principal) {
        Usuario tecnico = usuarioService.obtenerPorCorreo(principal.getName());

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket != null && ticket.getAsignadoA() == null) {
            ticketService.asignarTecnico(id, tecnico, tecnico);
            ticketService.cambiarEstado(id, EstadoTicket.EN_PROCESO, tecnico, "Ticket asignado y tomado en proceso", null);
        }

        return "redirect:/tecnico/ticket/" + id;
    }
}
