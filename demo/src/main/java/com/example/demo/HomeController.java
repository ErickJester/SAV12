package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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

    // =========================
    // REPOSITORY
    // =========================
    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private com.example.demo.service.EmailService emailService;

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

        Usuario usuario = null;
        try {
            usuario = usuarioService.obtenerPorCorreo(correo);
        } catch (DataAccessException dae) {
            logger.error("DB error while finding user by correo: {}", dae.getMessage());
            model.addAttribute("mensaje", "Error de conexión a la base de datos. Intenta de nuevo más tarde.");
            return "login";
        }

        if (usuario == null) {
            model.addAttribute("mensaje", "Correo no registrado");
            return "login";
        }

        if (!usuario.getPassword().equals(password)) {
            model.addAttribute("mensaje", "Contraseña incorrecta");
            return "login";
        }

        if (!usuario.getActivo()) {
            model.addAttribute("mensaje", "Usuario inactivo. Contacte al administrador.");
            return "login";
        }

        // Guardar usuario en sesión
        session.setAttribute("usuario", usuario);

        // Enviar correo de confirmación de inicio de sesión (si está habilitado)
        emailService.sendLoginConfirmation(usuario);

        // Redirigir según el rol
        switch (usuario.getRol()) {
            case USUARIO:
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

        // Validar correo duplicado
        if (usuarioService.existeCorreo(registro.getCorreo())) {
            model.addAttribute("mensaje", "El correo ya está registrado.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Validar selección de rol
        if (registro.getRol() == null || registro.getRol().isEmpty()) {
            model.addAttribute("mensaje", "Selecciona el tipo de cuenta.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Validar contraseñas coincidan
        if (registro.getPassword() == null || !registro.getPassword().equals(registro.getPassword2())) {
            model.addAttribute("mensaje", "Las contraseñas no coinciden.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Crear entidad Usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(registro.getNombre());
        usuario.setCorreo(registro.getCorreo());
        usuario.setPassword(registro.getPassword()); // luego se encripta
        try {
            usuario.setRol(Rol.valueOf(registro.getRol()));
        } catch (Exception e) {
            model.addAttribute("mensaje", "Tipo de cuenta inválido.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        // Asignar boleta o id_trabajador según rol
        if (registro.getRol().equals("USUARIO")) {
            usuario.setBoleta(registro.getBoleta());
            usuario.setIdTrabajador(null);
        } else {
            usuario.setIdTrabajador(registro.getIdTrabajador());
            usuario.setBoleta(null);
        }

        try {
            usuarioService.guardarUsuario(usuario);
            // Enviar correo de bienvenida tras registro exitoso
            emailService.sendWelcomeEmail(usuario);
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
