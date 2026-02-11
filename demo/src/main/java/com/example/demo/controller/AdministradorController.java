package com.example.demo.controller;

import com.example.demo.DTO.CategoriaDTO;
import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.DTO.UbicacionDTO;
import com.example.demo.entity.*;
import com.example.demo.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @GetMapping("/panel")
    public String panel(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        Map<String, Object> reporteGeneral = reporteService.generarReporteGeneral();
        model.addAttribute("usuario", admin);
        model.addAttribute("reporte", reporteGeneral);
        return "admin/panel";
    }

    @GetMapping("/usuarios")
    public String gestionUsuarios(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        List<Usuario> usuarios = usuarioService.obtenerTodosUsuarios();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("usuario", admin);
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/cambiar-estado")
    public String cambiarEstadoUsuario(@PathVariable Long id,
                                       @RequestParam Boolean activo,
                                       HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        usuarioService.cambiarEstadoUsuario(id, activo);
        return "redirect:/admin/usuarios?success=updated";
    }

    @PostMapping("/usuarios/{id}/cambiar-rol")
    public String cambiarRolUsuario(@PathVariable Long id,
                                    @RequestParam String rol,
                                    HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario != null) {
            try {
                Rol nuevoRol = Rol.valueOf(rol);
                usuario.setRol(nuevoRol);
                usuarioService.guardarUsuario(usuario);
            } catch (IllegalArgumentException ex) {
                // rol inválido -> no revienta, solo redirige con error
                return "redirect:/admin/usuarios?error=rolinvalido";
            }
        }
        return "redirect:/admin/usuarios?success=rolchanged";
    }

    @GetMapping("/categorias")
    public String gestionCategorias(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        List<Categoria> categorias = catalogoService.obtenerTodasCategorias();
        model.addAttribute("categorias", categorias);
        model.addAttribute("usuario", admin);
        model.addAttribute("nuevaCategoria", new CategoriaDTO());
        return "admin/categorias";
    }

    @PostMapping("/categorias/crear")
    public String crearCategoria(@ModelAttribute CategoriaDTO dto, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        catalogoService.crearCategoria(dto);
        return "redirect:/admin/categorias?success=created";
    }

    @PostMapping("/categorias/{id}/desactivar")
    public String desactivarCategoria(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        catalogoService.desactivarCategoria(id);
        return "redirect:/admin/categorias?success=deactivated";
    }

    @GetMapping("/ubicaciones")
    public String gestionUbicaciones(HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        List<Ubicacion> ubicaciones = catalogoService.obtenerTodasUbicaciones();
        model.addAttribute("ubicaciones", ubicaciones);
        model.addAttribute("usuario", admin);
        model.addAttribute("nuevaUbicacion", new UbicacionDTO());
        return "admin/ubicaciones";
    }

    @PostMapping("/ubicaciones/crear")
    public String crearUbicacion(@ModelAttribute UbicacionDTO dto, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        catalogoService.crearUbicacion(dto);
        return "redirect:/admin/ubicaciones?success=created";
    }

    @PostMapping("/ubicaciones/{id}/desactivar")
    public String desactivarUbicacion(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        catalogoService.desactivarUbicacion(id);
        return "redirect:/admin/ubicaciones?success=deactivated";
    }

    @GetMapping("/reportes")
    public String reportes(HttpSession session, Model model,
                           @RequestParam(required = false) String periodo,
                           @RequestParam(required = false) String desde,
                           @RequestParam(required = false) String hasta) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        // Normaliza periodo (UI y backend alineados)
        if (periodo == null || periodo.isBlank()) {
            periodo = "semanal";
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime hastaDate = ahora;
        LocalDateTime desdeDate;

        switch (periodo.toLowerCase()) {
            case "mensual":
                desdeDate = ahora.minusMonths(1);
                break;

            case "trimestral":
                desdeDate = ahora.minusMonths(3);
                break;

            case "anual":
                desdeDate = ahora.minusYears(1);
                break;

            case "custom":
                if (desde != null && !desde.isBlank() && hasta != null && !hasta.isBlank()) {
                    try {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        LocalDate d = LocalDate.parse(desde, fmt);
                        LocalDate h = LocalDate.parse(hasta, fmt);
                        desdeDate = d.atStartOfDay();
                        hastaDate = h.atTime(LocalTime.MAX);
                    } catch (Exception ex) {
                        // fechas inválidas -> fallback semanal
                        periodo = "semanal";
                        desde = null;
                        hasta = null;
                        desdeDate = ahora.minusWeeks(1);
                    }
                } else {
                    // custom incompleto -> fallback semanal
                    periodo = "semanal";
                    desde = null;
                    hasta = null;
                    desdeDate = ahora.minusWeeks(1);
                }
                break;

            case "semanal":
            default:
                desdeDate = ahora.minusWeeks(1);
                break;
        }

        // Un solo camino: siempre por periodo
        Map<String, Object> reporteSLA = reporteService.generarReporteSLAPorPeriodo(desdeDate, hastaDate);
        Map<String, Long> reportePorEstado = reporteService.generarReportePorEstadoPorPeriodo(desdeDate, hastaDate);
        Map<String, Object> reporteGeneral = reporteService.generarReporteGeneralPorPeriodo(desdeDate, hastaDate);
        List<Map<String, Object>> topCategorias = reporteService.generarTopCategoriasPorPeriodo(desdeDate, hastaDate);

        // Siempre setearlo para el selector
        model.addAttribute("periodoSeleccionado", periodo);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        model.addAttribute("reporteSLA", reporteSLA);
        model.addAttribute("reportePorEstado", reportePorEstado);
        model.addAttribute("reporteGeneral", reporteGeneral);
        model.addAttribute("topCategorias", topCategorias);
        model.addAttribute("usuario", admin);

        return "admin/reportes";
    }

    @GetMapping("/reportes/export")
    public ResponseEntity<byte[]> exportarReporteCSV(
            @RequestParam(required = false, defaultValue = "semanal") String periodo,
            HttpSession session) {
        
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Calcular fechas según el periodo (misma lógica que en /reportes)
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime hastaDate = ahora;
        LocalDateTime desdeDate;

        switch (periodo.toLowerCase()) {
            case "mensual":
                desdeDate = ahora.minusMonths(1);
                break;
            case "trimestral":
                desdeDate = ahora.minusMonths(3);
                break;
            case "anual":
                desdeDate = ahora.minusYears(1);
                break;
            case "semanal":
            default:
                desdeDate = ahora.minusWeeks(1);
                break;
        }

        // Obtener datos de los reportes
        Map<String, Object> reporteSLA = reporteService.generarReporteSLAPorPeriodo(desdeDate, hastaDate);
        Map<String, Object> reporteGeneral = reporteService.generarReporteGeneralPorPeriodo(desdeDate, hastaDate);
        List<Map<String, Object>> topCategorias = reporteService.generarTopCategoriasPorPeriodo(desdeDate, hastaDate);

        // Construir CSV
        StringBuilder csv = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        // Encabezado del reporte
        csv.append("REPORTE DE TICKETS - PERIODO: ").append(periodo.toUpperCase()).append("\n");
        csv.append("Generado: ").append(ahora.format(formatter)).append("\n");
        csv.append("Rango: ").append(desdeDate.format(formatter))
           .append(" - ").append(hastaDate.format(formatter)).append("\n");
        csv.append("\n");

        // Sección: Tickets General
        csv.append("=== TICKETS GENERAL ===\n");
        csv.append("Métrica,Valor\n");
        csv.append("Total tickets,").append(reporteGeneral.get("totalTickets")).append("\n");
        csv.append("Tickets abiertos,").append(reporteGeneral.get("ticketsAbiertos")).append("\n");
        csv.append("Tickets reabiertos,").append(reporteGeneral.get("ticketsReabiertos")).append("\n");
        csv.append("Tickets en proceso,").append(reporteGeneral.get("ticketsEnProceso")).append("\n");
        csv.append("Tickets en espera,").append(reporteGeneral.get("ticketsEnEspera")).append("\n");
        csv.append("Tickets resueltos,").append(reporteGeneral.get("ticketsResueltos")).append("\n");
        csv.append("Tickets cerrados,").append(reporteGeneral.get("ticketsCerrados")).append("\n");
        csv.append("Tickets cancelados,").append(reporteGeneral.get("ticketsCancelados")).append("\n");
        csv.append("\n");

        // Sección: SLA Primera Respuesta
        csv.append("=== SLA PRIMERA RESPUESTA ===\n");
        csv.append("Métrica,Valor\n");
        csv.append("Cumplimiento (%),").append(reporteSLA.get("slaPrimeraRespuestaPorcentaje")).append("%\n");
        csv.append("Tickets cumplen SLA,").append(reporteSLA.get("ticketsCumplenPrimeraRespuesta")).append("\n");
        csv.append("Tickets incumplen SLA,").append(reporteSLA.get("ticketsIncumplenPrimeraRespuesta")).append("\n");
        csv.append("Total tickets analizados,").append(reporteSLA.get("totalTickets")).append("\n");
        csv.append("\n");

        // Sección: SLA Resolución
        csv.append("=== SLA RESOLUCIÓN ===\n");
        csv.append("Métrica,Valor\n");
        csv.append("Cumplimiento (%),").append(reporteSLA.get("slaResolucionPorcentaje")).append("%\n");
        csv.append("Tickets cumplen SLA,").append(reporteSLA.get("ticketsCumplenResolucion")).append("\n");
        csv.append("Tickets incumplen SLA,").append(reporteSLA.get("ticketsIncumplenResolucion")).append("\n");
        csv.append("Total tickets analizados,").append(reporteSLA.get("totalTickets")).append("\n");
        csv.append("\n");

        // Sección: Top Categorías
        csv.append("=== TOP CATEGORÍAS ===\n");
        csv.append("Categoría,Cantidad\n");
        for (Map<String, Object> cat : topCategorias) {
            csv.append(cat.get("nombre")).append(",").append(cat.get("total")).append("\n");
        }

        // Preparar respuesta
        byte[] csvBytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        String filename = "reporte_" + periodo + "_" + 
                         LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=utf-8"));
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename(filename)
                .build());
        headers.add("Content-Type", "text/csv; charset=utf-8");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @GetMapping("/tickets")
    public String verTodosTickets(HttpSession session, Model model,
                                  @RequestParam(required = false) String filtro) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        List<Ticket> tickets = ticketService.obtenerTodosLosTickets();

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
                        .filter(t -> t.getEstado() == EstadoTicket.RESUELTO
                                || t.getEstado() == EstadoTicket.CERRADO)
                        .collect(Collectors.toList());
            }
        }

        // Esto es lo que la vista espera como "asignables"
        List<Usuario> asignables = usuarioService.obtenerUsuariosAsignables();

        model.addAttribute("tickets", tickets);
        model.addAttribute("asignables", asignables);
        model.addAttribute("usuario", admin);
        model.addAttribute("filtro", filtro == null ? "all" : filtro);
        return "admin/tickets";
    }

    @GetMapping("/ticket/{id}")
    public String verDetalleTicket(@PathVariable Long id, HttpSession session, Model model) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket == null) return "redirect:/admin/tickets?error=notfound";

        List<Comentario> comentarios = comentarioService.obtenerComentariosDeTicket(ticket);
        List<HistorialAccion> historial = ticketService.obtenerHistorialDeTicket(ticket);

        model.addAttribute("ticket", ticket);
        model.addAttribute("comentarios", comentarios);
        model.addAttribute("historial", historial);
        model.addAttribute("usuario", admin);
        return "admin/detalle-ticket";
    }

    @PostMapping("/ticket/{id}/comentar")
    public String agregarComentario(@PathVariable Long id,
                                    @RequestParam String contenido,
                                    HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        ComentarioDTO dto = new ComentarioDTO();
        dto.setTicketId(id);
        dto.setContenido(contenido);

        comentarioService.agregarComentario(dto, admin);
        return "redirect:/admin/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/reabrir")
    public String reabrirTicket(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        try {
            ticketService.reabrirTicket(id, admin);
        } catch (Exception e) {
            return "redirect:/admin/ticket/" + id + "?error=reabrir";
        }
        return "redirect:/admin/ticket/" + id;
    }

    @PostMapping("/tickets/{id}/asignarme")
    public String asignarmeTicket(@PathVariable Long id, HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket != null) {
            ticketService.asignarTecnico(id, admin, admin);
        }

        return "redirect:/admin/ticket/" + id;
    }

    @PostMapping("/tickets/{id}/asignar-tecnico")
    public String asignarTecnico(@PathVariable Long id,
                                 @RequestParam Long tecnicoId,
                                 HttpSession session) {
        Usuario admin = (Usuario) session.getAttribute("usuario");
        if (admin == null || admin.getRol() != Rol.ADMIN) return "redirect:/login";

        Usuario tecnico = usuarioService.obtenerPorId(tecnicoId);
        if (tecnico != null && (tecnico.getRol() == Rol.TECNICO || tecnico.getRol() == Rol.ADMIN)) {
            ticketService.asignarTecnico(id, tecnico, admin);
        }

        return "redirect:/admin/tickets?success=assigned";
    }
}
