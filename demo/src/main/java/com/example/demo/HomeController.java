package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.DTO.RegistroDTO;
import com.example.demo.DTO.ReporteDTO;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.service.UsuarioService;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private com.example.demo.service.EmailService emailService;

    // =========================
    // helpers
    // =========================
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String norm(String s) {
        return isBlank(s) ? null : s.trim();
    }

    // =========================
    // RUTA RAÍZ → LOGIN
    // =========================
    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    // =========================
    // LOGIN (GET)
    // =========================
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // =========================
    // LOGIN (POST)
    // =========================
    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String correo,
            @RequestParam String password,
            HttpSession session,
            Model model
    ) {
        String correoNorm = norm(correo);

        if (correoNorm == null) {
            model.addAttribute("mensaje", "Correo requerido");
            return "login";
        }

        Usuario usuario = null;
        try {
            usuario = usuarioService.obtenerPorCorreo(correoNorm);
        } catch (DataAccessException dae) {
            logger.error("DB error while finding user by correo: {}", dae.getMessage());
            model.addAttribute("mensaje", "Error de conexión a la base de datos. Intenta de nuevo más tarde.");
            return "login";
        }

        if (usuario == null) {
            model.addAttribute("mensaje", "Correo no registrado");
            return "login";
        }

        // Nota: lo de password hash lo dejas para el final, así que aquí sigue el equals crudo.
        if (!usuario.getPasswordHash().equals(password)) {
            model.addAttribute("mensaje", "Contraseña incorrecta");
            return "login";
        }

        if (!usuario.getActivo()) {
            model.addAttribute("mensaje", "Usuario inactivo. Contacte al administrador.");
            return "login";
        }

        session.setAttribute("usuario", usuario);

        emailService.sendLoginConfirmation(usuario);

        switch (usuario.getRol()) {
            case ALUMNO:
            case DOCENTE:
            case ADMINISTRATIVO:
                return "redirect:/usuario/panel";
            case TECNICO:
                return "redirect:/tecnico/panel";
            case ADMIN:
                return "redirect:/admin/panel";
            default:
                return "redirect:/login";
        }
    }

    // =========================
    // LOGOUT
    // =========================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    // =========================
    // REGISTRO (GET)
    // =========================
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registro", new RegistroDTO());
        return "registro";
    }

    // =========================
    // REGISTRO (POST)
    // =========================
    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute RegistroDTO registro, Model model) {

        // --- normalización ---
        String nombre = norm(registro.getNombre());
        String correo = norm(registro.getCorreo());
        String rolRaw = norm(registro.getRol());
        String boleta = norm(registro.getBoleta());
        String idTrab = norm(registro.getIdTrabajador());

        // Validaciones básicas mínimas (sin inventarte “validaciones bonitas”)
        if (nombre == null) {
            model.addAttribute("mensaje", "Nombre requerido.");
            model.addAttribute("registro", registro);
            return "registro";
        }
        if (correo == null) {
            model.addAttribute("mensaje", "Correo requerido.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Validar selección de rol
        if (rolRaw == null) {
            model.addAttribute("mensaje", "Selecciona el tipo de cuenta.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Parse rol a enum (acepta si viene en mayúsculas; si no, lo forzamos)
        Rol rol;
        try {
            rol = Rol.valueOf(rolRaw.trim().toUpperCase());
        } catch (Exception e) {
            model.addAttribute("mensaje", "Tipo de cuenta inválido.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Validar contraseñas coincidan
        if (registro.getPassword() == null || !registro.getPassword().equals(registro.getPassword2())) {
            model.addAttribute("mensaje", "Las contraseñas no coinciden.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Validar correo duplicado (usando correo normalizado)
        if (usuarioService.existeCorreo(correo)) {
            model.addAttribute("mensaje", "El correo ya está registrado.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // =========================
        // VALIDACIÓN FUERTE POR ROL
        // =========================
        if (rol == Rol.ALUMNO) {

            if (boleta == null) {
                model.addAttribute("mensaje", "Boleta requerida para cuenta ALUMNO.");
                model.addAttribute("registro", registro);
                return "registro";
            }
            if (idTrab != null) {
                model.addAttribute("mensaje", "No debes capturar ID de trabajador para cuenta ALUMNO.");
                model.addAttribute("registro", registro);
                return "registro";
            }
            if (usuarioService.existeBoleta(boleta)) {
                model.addAttribute("mensaje", "La boleta ya está registrada.");
                model.addAttribute("registro", registro);
                return "registro";
            }

        } else { // DOCENTE / TECNICO / ADMIN / ADMINISTRATIVO

            if (idTrab == null) {
                model.addAttribute("mensaje", "ID de trabajador requerido para este tipo de cuenta.");
                model.addAttribute("registro", registro);
                return "registro";
            }
            if (boleta != null) {
                model.addAttribute("mensaje", "No debes capturar boleta para este tipo de cuenta.");
                model.addAttribute("registro", registro);
                return "registro";
            }
            if (usuarioService.existeIdTrabajador(idTrab)) {
                model.addAttribute("mensaje", "El ID de trabajador ya está registrado.");
                model.addAttribute("registro", registro);
                return "registro";
            }
        }

        // Crear entidad Usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setPasswordHash(registro.getPassword()); // lo dejas así por ahora
        usuario.setRol(rol);

        // Asignación consistente (nunca ambos)
        if (rol == Rol.ALUMNO) {
            usuario.setBoleta(boleta);
            usuario.setIdTrabajador(null);
        } else {
            usuario.setIdTrabajador(idTrab);
            usuario.setBoleta(null);
        }

        try {
            usuarioService.guardarUsuario(usuario);
            emailService.sendWelcomeEmail(usuario);

        } catch (DataIntegrityViolationException dive) {
            // si se coló por carrera o por UNIQUE constraints, lo manejas sin reventar
            logger.error("Integrity violation while saving new user: {}", dive.getMessage());
            model.addAttribute("mensaje", "Datos duplicados. Verifica correo/boleta/id de trabajador.");
            model.addAttribute("registro", registro);
            return "registro";

        } catch (DataAccessException dae) {
            logger.error("DB error while saving new user: {}", dae.getMessage());
            model.addAttribute("mensaje", "Error de conexión a la base de datos. Intenta de nuevo más tarde.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        return "redirect:/login?registro=success";
    }

    // =========================
    // LEVANTAR REPORTE (GET)
    // =========================
    @GetMapping("/levantar-reporte")
    public String mostrarFormularioReporte(Model model) {
        model.addAttribute("reporte", new ReporteDTO());
        return "levantar-reporte";
    }

    // =========================
    // LEVANTAR REPORTE (POST)
    // =========================
    @PostMapping("/levantar-reporte")
    public String guardarReporte(
            @ModelAttribute("reporte") ReporteDTO reporte,
            Model model
    ) {

        System.out.println("=== REPORTE RECIBIDO ===");
        System.out.println("Boleta: " + reporte.getBoleta());
        System.out.println("Salón: " + reporte.getSalon());
        System.out.println("Fecha: " + reporte.getFecha());
        System.out.println("Hora: " + reporte.getHora());
        System.out.println("Título: " + reporte.getTitulo());
        System.out.println("Descripción: " + reporte.getDescripcion());
        System.out.println("Prioridad: " + reporte.getPrioridad());

        model.addAttribute("mensaje", "Reporte enviado correctamente.");
        return "levantar-reporte";
    }
}
