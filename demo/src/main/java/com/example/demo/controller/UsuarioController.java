package com.example.demo.controller;

import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.DTO.TicketDTO;
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
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ComentarioService comentarioService;

    @Autowired
    private CatalogoService catalogoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private FileStorageService fileStorageService;

    // Panel principal del usuario
    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!esUsuarioPermitido(usuario)) {
            return "redirect:/login";
        }

        List<Ticket> misTickets = ticketService.obtenerTicketsDeUsuario(usuario);
        List<Ticket> ticketsOrdenados = misTickets.stream()
            .sorted(Comparator.comparing(Ticket::getFechaCreacion).reversed())
            .collect(Collectors.toList());
        model.addAttribute("usuario", usuario);
        model.addAttribute("tickets", ticketsOrdenados);
        return "usuario/panel";
    }

    // Formulario para crear ticket
    @GetMapping("/crear-ticket")
    public String mostrarFormularioCrearTicket(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!esUsuarioPermitido(usuario)) {
            return "redirect:/login";
        }

        model.addAttribute("categorias", catalogoService.obtenerCategoriasActivas());
        model.addAttribute("ubicaciones", catalogoService.obtenerUbicacionesActivas());
        model.addAttribute("ticket", new TicketDTO());
        return "usuario/crear-ticket";
    }

    // Procesar creación de ticket
    @PostMapping("/crear-ticket")
    public String crearTicket(@ModelAttribute TicketDTO ticketDTO, 
                             @RequestParam(value = "archivoEvidencia", required = false) org.springframework.web.multipart.MultipartFile archivo,
                             HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!esUsuarioPermitido(usuario)) {
            return "redirect:/login";
        }

        try {
            // Guardar archivo si existe
            if (archivo != null && !archivo.isEmpty()) {
                String nombreArchivo = fileStorageService.guardarArchivo(archivo);
                ticketDTO.setEvidenciaProblema(nombreArchivo);
            }
            
            ticketService.crearTicket(ticketDTO, usuario);
            return "redirect:/usuario/mis-tickets?success=created";
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear el ticket: " + e.getMessage());
            model.addAttribute("categorias", catalogoService.obtenerCategoriasActivas());
            model.addAttribute("ubicaciones", catalogoService.obtenerUbicacionesActivas());
            return "usuario/crear-ticket";
        }
    }

    // Consultar mis tickets
    @GetMapping("/mis-tickets")
    public String misTickets(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!esUsuarioPermitido(usuario)) {
            return "redirect:/login";
        }

        List<Ticket> tickets = ticketService.obtenerTicketsDeUsuario(usuario);
        model.addAttribute("tickets", tickets);
        model.addAttribute("usuario", usuario);
        return "usuario/mis-tickets";
    }

    // Ver detalle de un ticket
    @GetMapping("/ticket/{id}")
    public String verDetalleTicket(@PathVariable Long id, HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!esUsuarioPermitido(usuario)) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket == null || !ticket.getCreadoPor().getId().equals(usuario.getId())) {
            return "redirect:/usuario/mis-tickets?error=notfound";
        }

        List<Comentario> comentarios = comentarioService.obtenerComentariosDeTicket(ticket);
        List<HistorialAccion> historial = ticketService.obtenerHistorialDeTicket(ticket);

        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);
        model.addAttribute("historial", historial);
        model.addAttribute("usuario", usuario);
        return "usuario/detalle-ticket";
    }

    // Agregar comentario a un ticket
    @PostMapping("/ticket/{id}/comentar")
    public String agregarComentario(@PathVariable Long id, 
                                     @RequestParam String contenido,
                                     HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (!esUsuarioPermitido(usuario)) {
            return "redirect:/login";
        }

        ComentarioDTO dto = new ComentarioDTO();
        dto.setTicketId(id);
        dto.setContenido(contenido);

        comentarioService.agregarComentario(dto, usuario);
        return "redirect:/usuario/ticket/" + id;
    }

    // Reabrir ticket
    @PostMapping("/ticket/{id}/reabrir")
    public String reabrirTicket(@PathVariable Long id, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null || (!esUsuarioPermitido(usuario) && !esStaff(usuario))) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket != null
                && (ticket.getCreadoPor().getId().equals(usuario.getId()) || esStaff(usuario))
                && (ticket.getEstado() == EstadoTicket.CERRADO || ticket.getEstado() == EstadoTicket.RESUELTO)) {
            ticketService.reabrirTicket(id, usuario);
        }

        return "redirect:/usuario/ticket/" + id;
    }

    private boolean esUsuarioPermitido(Usuario usuario) {
        return usuario != null && (usuario.getRol() == Rol.ALUMNO
                || usuario.getRol() == Rol.DOCENTE
                || usuario.getRol() == Rol.ADMINISTRATIVO);
    }

    private boolean esStaff(Usuario usuario) {
        return usuario != null && (usuario.getRol() == Rol.ADMINISTRATIVO
                || usuario.getRol() == Rol.TECNICO
                || usuario.getRol() == Rol.ADMIN);
    }
}
