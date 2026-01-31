package com.example.demo.controller;

import com.example.demo.DTO.CategoriaDTO;
import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.DTO.UbicacionDTO;
import com.example.demo.entity.*;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin")
public class AdministradorController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CatalogoService catalogoService;

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private ComentarioService comentarioService;

    // Panel principal del administrador
    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Map<String, Object> reporteGeneral = reporteService.generarReporteGeneral();
        model.addAttribute("usuario", admin);
        model.addAttribute("reporte", reporteGeneral);
        return "admin/panel";
    }

    // Gestión de usuarios
    @GetMapping("/usuarios")
    public String gestionUsuarios(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        List<Usuario> usuarios = usuarioService.obtenerTodosUsuarios();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("usuario", admin);
        return "admin/usuarios";
    }

    // Cambiar estado de usuario
    @PostMapping("/usuarios/{id}/cambiar-estado")
    public String cambiarEstadoUsuario(@PathVariable Long id, 
                                       @RequestParam Boolean activo,
                                       HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        usuarioService.cambiarEstadoUsuario(id, activo);
        return "redirect:/admin/usuarios?success=updated";
    }

    // Cambiar rol de usuario
    @PostMapping("/usuarios/{id}/cambiar-rol")
    public String cambiarRolUsuario(@PathVariable Long id,
                                    @RequestParam String rol,
                                    HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario != null) {
            usuario.setRol(Rol.valueOf(rol));
            usuarioService.guardarUsuario(usuario);
        }

        return "redirect:/admin/usuarios?success=rolchanged";
    }

    // Gestión de categorías
    @GetMapping("/categorias")
    public String gestionCategorias(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        List<Categoria> categorias = catalogoService.obtenerTodasCategorias();
        model.addAttribute("categorias", categorias);
        model.addAttribute("usuario", admin);
        model.addAttribute("nuevaCategoria", new CategoriaDTO());
        return "admin/categorias";
    }

    // Crear categoría
    @PostMapping("/categorias/crear")
    public String crearCategoria(@ModelAttribute CategoriaDTO dto, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        catalogoService.crearCategoria(dto);
        return "redirect:/admin/categorias?success=created";
    }

    // Desactivar categoría
    @PostMapping("/categorias/{id}/desactivar")
    public String desactivarCategoria(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        catalogoService.desactivarCategoria(id);
        return "redirect:/admin/categorias?success=deactivated";
    }

    // Gestión de ubicaciones
    @GetMapping("/ubicaciones")
    public String gestionUbicaciones(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        List<Ubicacion> ubicaciones = catalogoService.obtenerTodasUbicaciones();
        model.addAttribute("ubicaciones", ubicaciones);
        model.addAttribute("usuario", admin);
        model.addAttribute("nuevaUbicacion", new UbicacionDTO());
        return "admin/ubicaciones";
    }

    // Crear ubicación
    @PostMapping("/ubicaciones/crear")
    public String crearUbicacion(@ModelAttribute UbicacionDTO dto, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        catalogoService.crearUbicacion(dto);
        return "redirect:/admin/ubicaciones?success=created";
    }

    // Desactivar ubicación
    @PostMapping("/ubicaciones/{id}/desactivar")
    public String desactivarUbicacion(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        catalogoService.desactivarUbicacion(id);
        return "redirect:/admin/ubicaciones?success=deactivated";
    }

    // Reportes SLA
    @GetMapping("/reportes")
    public String reportes(HttpSession session, Model model,
                           @RequestParam(required = false) String periodo,
                           @RequestParam(required = false) String desde,
                           @RequestParam(required = false) String hasta) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }
        Map<String, Object> reporteSLA;
        Map<String, Long> reportePorEstado;
        Map<String, Object> reporteGeneral;
        List<Map<String, Object>> topCategorias;

        // If period specified, compute date range
        if (periodo != null) {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime desdeDate = ahora.minusWeeks(1);
            LocalDateTime hastaDate = ahora;

            if ("mensual".equalsIgnoreCase(periodo)) {
                desdeDate = ahora.minusMonths(1);
            } else if ("semanal".equalsIgnoreCase(periodo)) {
                desdeDate = ahora.minusWeeks(1);
            } else if ("trimestral".equalsIgnoreCase(periodo)) {
                desdeDate = ahora.minusMonths(3);
            } else if ("trimestral".equalsIgnoreCase(periodo)) {
                desdeDate = ahora.minusMonths(3);
            } else if ("custom".equalsIgnoreCase(periodo) && desde != null && hasta != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate d = LocalDate.parse(desde, fmt);
                LocalDate h = LocalDate.parse(hasta, fmt);
                desdeDate = d.atStartOfDay();
                hastaDate = h.atTime(LocalTime.MAX);
            }

            reporteSLA = reporteService.generarReporteSLAPorPeriodo(desdeDate, hastaDate);
            reportePorEstado = reporteService.generarReportePorEstadoPorPeriodo(desdeDate, hastaDate);
            reporteGeneral = reporteService.generarReporteGeneralPorPeriodo(desdeDate, hastaDate);
            topCategorias = reporteService.generarTopCategoriasPorPeriodo(desdeDate, hastaDate);
            model.addAttribute("periodoSeleccionado", periodo);
            model.addAttribute("desde", desde);
            model.addAttribute("hasta", hasta);
        } else {
            reporteSLA = reporteService.generarReporteSLA();
            reportePorEstado = reporteService.generarReportePorEstado();
            reporteGeneral = reporteService.generarReporteGeneral();
            topCategorias = reporteService.generarTopCategorias();
        }

        model.addAttribute("reporteSLA", reporteSLA);
        model.addAttribute("reportePorEstado", reportePorEstado);
        model.addAttribute("reporteGeneral", reporteGeneral);
        model.addAttribute("topCategorias", topCategorias);
        model.addAttribute("usuario", admin);
        return "admin/reportes";
    }

    // Ver todos los tickets
    @GetMapping("/tickets")
    public String verTodosTickets(HttpSession session, Model model,
                                  @RequestParam(required = false) String filtro) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }
        List<Ticket> tickets = ticketService.obtenerTodosLosTickets();
        // Apply filter if provided: active (ABIERTO, EN_PROCESO), resolved (RESUELTO)
        if (filtro != null) {
            if ("activo".equalsIgnoreCase(filtro)) {
                tickets = tickets.stream()
                        .filter(t -> t.getEstado() == EstadoTicket.ABIERTO
                                || t.getEstado() == EstadoTicket.REABIERTO
                                || t.getEstado() == EstadoTicket.EN_PROCESO
                                || t.getEstado() == EstadoTicket.EN_ESPERA)
                        .collect(Collectors.toList());
            } else if ("resuelto".equalsIgnoreCase(filtro)) {
                tickets = tickets.stream()
                        .filter(t -> t.getEstado() == EstadoTicket.RESUELTO || t.getEstado() == EstadoTicket.CERRADO)
                        .collect(Collectors.toList());
            }
        }

        List<Usuario> tecnicos = usuarioService.obtenerUsuariosAsignables();

        model.addAttribute("tickets", tickets);
        model.addAttribute("asignables", asignables);
        model.addAttribute("usuario", admin);
        model.addAttribute("filtro", filtro == null ? "all" : filtro);
        return "admin/tickets";
    }

    // Ver detalle de un ticket
    @GetMapping("/ticket/{id}")
    public String verDetalleTicket(@PathVariable Long id, HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket == null) {
            return "redirect:/admin/tickets?error=notfound";
        }

        List<Comentario> comentarios = comentarioService.obtenerComentariosDeTicket(ticket);
        List<HistorialAccion> historial = ticketService.obtenerHistorialDeTicket(ticket);

        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);
        model.addAttribute("historial", historial);
        model.addAttribute("usuario", admin);
        return "admin/detalle-ticket";
    }

    // Agregar comentario a un ticket como administrador
    @PostMapping("/ticket/{id}/comentar")
    public String agregarComentario(@PathVariable Long id,
                                     @RequestParam String contenido,
                                     HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        ComentarioDTO dto = new ComentarioDTO();
        dto.setTicketId(id);
        dto.setContenido(contenido);

        comentarioService.agregarComentario(dto, admin);
        return "redirect:/admin/ticket/" + id;
    }

    // Reabrir ticket como administrador
    @PostMapping("/ticket/{id}/reabrir")
    public String reabrirTicket(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        try {
            ticketService.reabrirTicket(id, admin);
        } catch (Exception e) {
            return "redirect:/admin/ticket/" + id + "?error=reabrir";
        }
        return "redirect:/admin/ticket/" + id;
    }

    // Asignarme el ticket como administrador
    @PostMapping("/tickets/{id}/asignarme")
    public String asignarmeTicket(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket != null) {
            ticketService.asignarTecnico(id, admin, admin);
            ticketService.asignarTecnico(id, admin, admin);
        }

        return "redirect:/admin/ticket/" + id;
    }

    // Asignar técnico a ticket
    @PostMapping("/tickets/{id}/asignar-tecnico")
    public String asignarTecnico(@PathVariable Long id,
                                 @RequestParam Long tecnicoId,
                                 HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return "redirect:/login";
        }

        Usuario tecnico = usuarioService.obtenerPorId(tecnicoId);
        if (tecnico != null && (tecnico.getRol() == Rol.TECNICO || tecnico.getRol() == Rol.ADMIN)) {
            ticketService.asignarTecnico(id, tecnico, admin);
        }

        return "redirect:/admin/tickets?success=assigned";
    }
}
