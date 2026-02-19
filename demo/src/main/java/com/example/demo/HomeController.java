package com.example.demo;

import com.example.demo.DTO.RegistroDTO;
import com.example.demo.DTO.ReporteDTO;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private com.example.demo.service.EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String norm(String s) {
        return isBlank(s) ? null : s.trim();
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String homeAfterLogin() {
        return "redirect:/usuario/panel";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("registro", new RegistroDTO());
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute RegistroDTO registro, Model model) {
        String nombre = norm(registro.getNombre());
        String correo = norm(registro.getCorreo());
        String rolRaw = norm(registro.getRol());
        String boleta = norm(registro.getBoleta());
        String idTrab = norm(registro.getIdTrabajador());

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
        if (rolRaw == null) {
            model.addAttribute("mensaje", "Selecciona el tipo de cuenta.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        Rol rol;
        try {
            rol = Rol.valueOf(rolRaw.trim().toUpperCase());
        } catch (Exception e) {
            model.addAttribute("mensaje", "Tipo de cuenta inválido.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        if (registro.getPassword() == null || !registro.getPassword().equals(registro.getPassword2())) {
            model.addAttribute("mensaje", "Las contraseñas no coinciden.");
            model.addAttribute("registro", registro);
            return "registro";
        }

        if (usuarioService.existeCorreo(correo)) {
            model.addAttribute("mensaje", "El correo ya está registrado.");
            model.addAttribute("registro", registro);
            return "registro";
        }

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
        } else {
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

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode(registro.getPassword()));
        usuario.setRol(rol);

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

    @GetMapping("/levantar-reporte")
    public String mostrarFormularioReporte(Model model) {
        model.addAttribute("reporte", new ReporteDTO());
        return "levantar-reporte";
    }

    @PostMapping("/levantar-reporte")
    public String guardarReporte(@ModelAttribute("reporte") ReporteDTO reporte, Model model) {
        model.addAttribute("mensaje", "Reporte enviado correctamente.");
        return "levantar-reporte";
    }
}
