package com.example.demo.controller;

import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.DTO.TicketDTO;
import com.example.demo.entity.Comentario;
import com.example.demo.entity.EstadoTicket;
import com.example.demo.entity.HistorialAccion;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.service.CatalogoService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

    private final TicketService ticketService;
    private final ComentarioService comentarioService;
    private final CatalogoService catalogoService;
    private final UsuarioService usuarioService;
    private final FileStorageService fileStorageService;

    public UsuarioController(
            TicketService ticketService,
            ComentarioService comentarioService,
            CatalogoService catalogoService,
            UsuarioService usuarioService,
            FileStorageService fileStorageService
    ) {
        this.ticketService = ticketService;
        this.comentarioService = comentarioService;
        this.catalogoService = catalogoService;
        this.usuarioService = usuarioService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/panel")
    public String panel(java.security.Principal principal, Model model) {
        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        if (!esUsuarioFinal(usuario)) {
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

    @GetMapping("/crear-ticket")
    public String mostrarFormularioCrearTicket(java.security.Principal principal, Model model) {
        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        if (!esUsuarioFinal(usuario)) {
            return "redirect:/login";
        }

        model.addAttribute("categorias", catalogoService.obtenerCategoriasActivas());
        model.addAttribute("ubicaciones", catalogoService.obtenerUbicacionesActivas());
        model.addAttribute("ticket", new TicketDTO());
        return "usuario/crear-ticket";
    }

    @PostMapping("/crear-ticket")
    public String crearTicket(
            @ModelAttribute TicketDTO ticketDTO,
            @RequestParam(value = "archivoEvidencia", required = false) MultipartFile archivo,
            java.security.Principal principal,
            Model model
    ) {
        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        if (!esUsuarioFinal(usuario)) {
            return "redirect:/login";
        }

        try {
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

    @GetMapping("/mis-tickets")
    public String misTickets(java.security.Principal principal, Model model) {
        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        if (!esUsuarioFinal(usuario)) {
            return "redirect:/login";
        }

        List<Ticket> tickets = ticketService.obtenerTicketsDeUsuario(usuario);
        model.addAttribute("tickets", tickets);
        model.addAttribute("usuario", usuario);
        return "usuario/mis-tickets";
    }

    @GetMapping("/ticket/{id}")
    public String verDetalleTicket(@PathVariable Long id, java.security.Principal principal, Model model) {
        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        if (!esUsuarioFinal(usuario)) {
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

    @PostMapping("/ticket/{id}/comentar")
    public String agregarComentario(@PathVariable Long id, @RequestParam String contenido, java.security.Principal principal) {
        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        if (!esUsuarioFinal(usuario)) {
            return "redirect:/login";
        }

        ComentarioDTO dto = new ComentarioDTO();
        dto.setTicketId(id);
        dto.setContenido(contenido);

        comentarioService.agregarComentario(dto, usuario);
        return "redirect:/usuario/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/reabrir")
    public String reabrirTicket(@PathVariable Long id, java.security.Principal principal) {
        Usuario usuario = usuarioService.obtenerPorCorreo(principal.getName());
        if (!esUsuarioFinal(usuario)) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket != null && ticket.getCreadoPor().getId().equals(usuario.getId())) {
            if (ticket.getEstado() == EstadoTicket.CERRADO
                    || ticket.getEstado() == EstadoTicket.RESUELTO
                    || ticket.getEstado() == EstadoTicket.CANCELADO) {
                ticketService.reabrirTicket(id, usuario);
            }
        }

        return "redirect:/usuario/ticket/" + id;
    }

    private boolean esUsuarioFinal(Usuario usuario) {
        return usuario != null && (usuario.getRol() == Rol.ALUMNO
                || usuario.getRol() == Rol.DOCENTE
                || usuario.getRol() == Rol.ADMINISTRATIVO);
    }
}
