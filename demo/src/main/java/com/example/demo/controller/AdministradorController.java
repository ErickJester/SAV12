package com.example.demo.controller;

import com.example.demo.DTO.CategoriaDTO;
import com.example.demo.DTO.ComentarioDTO;
import com.example.demo.DTO.UbicacionDTO;
import com.example.demo.entity.*;
import com.example.demo.service.*;
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
import java.util.HashMap;
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

    @Autowired
    private ExportService exportService;

    @GetMapping("/panel")
    public String panel(java.security.Principal principal, Model model) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        Map<String, Object> reporteGeneral = reporteService.generarReporteGeneral();
        model.addAttribute("usuario", admin);
        model.addAttribute("reporte", reporteGeneral);
        return "admin/panel";
    }

    @GetMapping("/usuarios")
    public String gestionUsuarios(java.security.Principal principal, Model model) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        List<Usuario> usuarios = usuarioService.obtenerTodosUsuarios();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("usuario", admin);
        return "admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/cambiar-estado")
    public String cambiarEstadoUsuario(@PathVariable Long id,
                                       @RequestParam Boolean activo,
                                       java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        usuarioService.cambiarEstadoUsuario(id, activo);
        return "redirect:/admin/usuarios?success=updated";
    }

    @PostMapping("/usuarios/{id}/cambiar-rol")
    public String cambiarRolUsuario(@PathVariable Long id,
                                    @RequestParam String rol,
                                    java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario != null) {
            try {
                Rol nuevoRol = Rol.valueOf(rol);
                usuario.setRol(nuevoRol);
                usuarioService.guardarUsuario(usuario);
            } catch (IllegalArgumentException ex) {
                return "redirect:/admin/usuarios?error=rolinvalido";
            }
        }
        return "redirect:/admin/usuarios?success=rolchanged";
    }

    @GetMapping("/categorias")
    public String gestionCategorias(java.security.Principal principal, Model model) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        List<Categoria> categorias = catalogoService.obtenerTodasCategorias();
        model.addAttribute("categorias", categorias);
        model.addAttribute("usuario", admin);
        model.addAttribute("nuevaCategoria", new CategoriaDTO());
        return "admin/categorias";
    }

    @PostMapping("/categorias/crear")
    public String crearCategoria(@ModelAttribute CategoriaDTO dto, java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        catalogoService.crearCategoria(dto);
        return "redirect:/admin/categorias?success=created";
    }

    @PostMapping("/categorias/{id}/desactivar")
    public String desactivarCategoria(@PathVariable Long id, java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        catalogoService.desactivarCategoria(id);
        return "redirect:/admin/categorias?success=deactivated";
    }

    @GetMapping("/ubicaciones")
    public String gestionUbicaciones(java.security.Principal principal, Model model) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        List<Ubicacion> ubicaciones = catalogoService.obtenerTodasUbicaciones();
        model.addAttribute("ubicaciones", ubicaciones);
        model.addAttribute("usuario", admin);
        model.addAttribute("nuevaUbicacion", new UbicacionDTO());
        return "admin/ubicaciones";
    }

    @PostMapping("/ubicaciones/crear")
    public String crearUbicacion(@ModelAttribute UbicacionDTO dto, java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        catalogoService.crearUbicacion(dto);
        return "redirect:/admin/ubicaciones?success=created";
    }

    @PostMapping("/ubicaciones/{id}/desactivar")
    public String desactivarUbicacion(@PathVariable Long id, java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        catalogoService.desactivarUbicacion(id);
        return "redirect:/admin/ubicaciones?success=deactivated";
    }

    @GetMapping("/reportes")
    public String reportes(java.security.Principal principal, Model model,
                           @RequestParam(required = false) String periodo,
                           @RequestParam(required = false) String desde,
                           @RequestParam(required = false) String hasta) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        // Normaliza periodo
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
                        periodo = "semanal";
                        desde = null;
                        hasta = null;
                        desdeDate = ahora.minusWeeks(1);
                    }
                } else {
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

        // ===== OBTENER TODOS LOS REPORTES =====
        
        // KPIs Ejecutivos
        Map<String, Object> kpis = reporteService.generarKPIsEjecutivos(desdeDate, hastaDate);
        
        // Reportes existentes
        Map<String, Object> reporteSLA = reporteService.generarReporteSLAPorPeriodo(desdeDate, hastaDate);
        Map<String, Long> reportePorEstado = reporteService.generarReportePorEstadoPorPeriodo(desdeDate, hastaDate);
        Map<String, Object> reporteGeneral = reporteService.generarReporteGeneralPorPeriodo(desdeDate, hastaDate);
        List<Map<String, Object>> topCategorias = reporteService.generarTopCategoriasPorPeriodo(desdeDate, hastaDate);
        
        // Nuevos reportes
        Map<String, Object> analisisTiempos = reporteService.generarAnalisisTiempos(desdeDate, hastaDate);
        List<Map<String, Object>> desempenoTecnicos = reporteService.generarDesempenoTecnicos(desdeDate, hastaDate);
        List<Map<String, Object>> analisisPorPrioridad = reporteService.generarAnalisisPorPrioridad(desdeDate, hastaDate);
        List<Map<String, Object>> analisisPorUbicaciones = reporteService.generarAnalisisPorUbicaciones(desdeDate, hastaDate);
        Map<String, Object> alertas = reporteService.generarAlertas(desdeDate, hastaDate);
        Map<String, Object> ticketsProblematicos = reporteService.generarTopTicketsProblematicos(desdeDate, hastaDate);

        // Pasar todo al modelo
        model.addAttribute("periodoSeleccionado", periodo);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        // KPIs
        model.addAttribute("kpis", kpis);
        
        // Reportes base
        model.addAttribute("reporteSLA", reporteSLA);
        model.addAttribute("reportePorEstado", reportePorEstado);
        model.addAttribute("reporteGeneral", reporteGeneral);
        model.addAttribute("topCategorias", topCategorias);
        
        // Nuevos reportes
        model.addAttribute("analisisTiempos", analisisTiempos);
        model.addAttribute("desempenoTecnicos", desempenoTecnicos);
        model.addAttribute("analisisPorPrioridad", analisisPorPrioridad);
        model.addAttribute("analisisPorUbicaciones", analisisPorUbicaciones);
        model.addAttribute("alertas", alertas);
        model.addAttribute("ticketsProblematicos", ticketsProblematicos);
        
        model.addAttribute("usuario", admin);

        return "admin/reportes";
    }

    // ===== EXPORTACIÓN PDF =====
    @GetMapping("/reportes/export/pdf")
    public ResponseEntity<byte[]> exportarReportePDF(
            @RequestParam(defaultValue = "mensual") String periodo,
            java.security.Principal principal) {
        
        Usuario admin = currentAdmin(principal);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Obtener todos los datos del reporte
            Map<String, Object> datos = obtenerDatosReporte(periodo);
            
            // Generar PDF
            byte[] pdfBytes = exportService.generarReportePDF(datos);
            
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "reporte_sav12_" + periodo + "_" + 
                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== EXPORTACIÓN CSV =====
    @GetMapping("/reportes/export/csv")
    public ResponseEntity<String> exportarReporteCSV(
            @RequestParam(defaultValue = "mensual") String periodo,
            java.security.Principal principal) {
        
        Usuario admin = currentAdmin(principal);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Obtener todos los datos del reporte
            Map<String, Object> datos = obtenerDatosReporte(periodo);
            
            // Generar CSV
            String csv = exportService.generarReporteCSV(datos);
            
            // Configurar headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv; charset=utf-8"));
            String filename = "reporte_sav12_" + periodo + "_" + 
                             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body("\uFEFF" + csv); // BOM para UTF-8
                    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== MÉTODO AUXILIAR PARA CONSOLIDAR DATOS =====
    private Map<String, Object> obtenerDatosReporte(String periodo) {
        // Calcular fechas según el período
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

        // Consolidar todos los datos
        Map<String, Object> datos = new HashMap<>();
        datos.put("periodo", periodo);
        datos.put("kpis", reporteService.generarKPIsEjecutivos(desdeDate, hastaDate));
        datos.put("reporteSLA", reporteService.generarReporteSLAPorPeriodo(desdeDate, hastaDate));
        datos.put("analisisTiempos", reporteService.generarAnalisisTiempos(desdeDate, hastaDate));
        datos.put("desempenoTecnicos", reporteService.generarDesempenoTecnicos(desdeDate, hastaDate));
        datos.put("analisisPorPrioridad", reporteService.generarAnalisisPorPrioridad(desdeDate, hastaDate));
        datos.put("analisisPorUbicaciones", reporteService.generarAnalisisPorUbicaciones(desdeDate, hastaDate));
        datos.put("ticketsProblematicos", reporteService.generarTopTicketsProblematicos(desdeDate, hastaDate));
        datos.put("alertas", reporteService.generarAlertas(desdeDate, hastaDate));
        
        return datos;
    }

    @GetMapping("/tickets")
    public String verTodosTickets(java.security.Principal principal, Model model,
                                  @RequestParam(required = false) String filtro) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

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

        List<Usuario> asignables = usuarioService.obtenerUsuariosAsignables();

        model.addAttribute("tickets", tickets);
        model.addAttribute("asignables", asignables);
        model.addAttribute("usuario", admin);
        model.addAttribute("filtro", filtro == null ? "all" : filtro);
        return "admin/tickets";
    }

    @GetMapping("/ticket/{id}")
    public String verDetalleTicket(@PathVariable Long id, java.security.Principal principal, Model model) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

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
                                    java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        ComentarioDTO dto = new ComentarioDTO();
        dto.setTicketId(id);
        dto.setContenido(contenido);

        comentarioService.agregarComentario(dto, admin);
        return "redirect:/admin/ticket/" + id;
    }

    @PostMapping("/ticket/{id}/reabrir")
    public String reabrirTicket(@PathVariable Long id, java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        try {
            ticketService.reabrirTicket(id, admin);
        } catch (Exception e) {
            return "redirect:/admin/ticket/" + id + "?error=reabrir";
        }
        return "redirect:/admin/ticket/" + id;
    }

    @PostMapping("/tickets/{id}/asignarme")
    public String asignarmeTicket(@PathVariable Long id, java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        Ticket ticket = ticketService.obtenerTicketPorId(id);
        if (ticket != null) {
            ticketService.asignarTecnico(id, admin, admin);
        }

        return "redirect:/admin/ticket/" + id;
    }

    @PostMapping("/tickets/{id}/asignar-tecnico")
    public String asignarTecnico(@PathVariable Long id,
                                 @RequestParam Long tecnicoId,
                                 java.security.Principal principal) {
        Usuario admin = currentAdmin(principal);
        if (admin == null) return "redirect:/login";

        Usuario tecnico = usuarioService.obtenerPorId(tecnicoId);
        if (tecnico != null && (tecnico.getRol() == Rol.TECNICO || tecnico.getRol() == Rol.ADMIN)) {
            ticketService.asignarTecnico(id, tecnico, admin);
        }

        return "redirect:/admin/tickets?success=assigned";
    }

    @GetMapping
    public String index() {
        return "redirect:/admin/panel";
    }

    private Usuario currentAdmin(java.security.Principal principal) {
        if (principal == null) {
            return null;
        }
        Usuario admin = usuarioService.obtenerPorCorreo(principal.getName());
        if (admin == null || admin.getRol() != Rol.ADMIN) {
            return null;
        }
        return admin;
    }

}
