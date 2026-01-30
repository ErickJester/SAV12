package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Rol;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private void sendSimple(String to, String subject, String body) {
        if (!mailEnabled || mailSender == null) {
            System.out.println("[EmailService] Mail disabled or JavaMailSender not available - would send to: " + to + " subj: " + subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            // Usa aquí tu correo de Gmail:
            helper.setFrom("elias015serrano@gmail.com");
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to build email to " + to + ": " + e.getMessage());
        } catch (MailException me) {
            System.err.println("Mail send error for " + to + ": " + me.getMessage());
        } catch (Exception ex) {
            System.err.println("Unexpected error sending email to " + to + ": " + ex.getMessage());
        }
    }

    public void sendWelcomeEmail(Usuario usuario) {
        if (usuario == null || usuario.getCorreo() == null) return;
        String subject = "¡Bienvenido a Quality ESCOM!";
        String body = "Hola " + usuario.getNombre() + ",\n\nTu cuenta ha sido creada exitosamente. Ya puedes iniciar sesión en el sistema.\n\nSaludos.";
        sendSimple(usuario.getCorreo(), subject, body);
    }

    public void sendLoginConfirmation(Usuario usuario) {
        if (usuario == null || usuario.getCorreo() == null) return;
        String subject = "Inicio de sesión exitoso";
        String body = "Hola " + usuario.getNombre() + ",\n\nSe ha detectado un inicio de sesión en su cuenta. Si no fuiste tú, por favor contacta al administrador.\n\nSaludos.";
        sendSimple(usuario.getCorreo(), subject, body);
    }

    public void notifyTechniciansOnNewTicket(Ticket ticket) {
        if (ticket == null) return;
        List<Usuario> tecnicos = usuarioRepository.findByRol(Rol.TECNICO);
        String subject = "Nuevo reporte creado: " + ticket.getTitulo();
        String body = "Se ha creado un nuevo reporte.\n\nTítulo: " + ticket.getTitulo() + "\nDescripción: " + ticket.getDescripcion() + "\nPrioridad: " + (ticket.getPrioridad() != null ? ticket.getPrioridad() : "N/A") + "\n\nPor favor revisa el sistema para más detalles.";

        for (Usuario t : tecnicos) {
            if (t.getCorreo() != null) sendSimple(t.getCorreo(), subject, body);
        }
    }

    public void notifyUserOnTicketChange(Ticket ticket, String detalles) {
        if (ticket == null || ticket.getCreadoPor() == null) return;
        Usuario u = ticket.getCreadoPor();
        if (u.getCorreo() == null) return;
        String subject = "Actualización en tu ticket: " + ticket.getTitulo();
        String body = "Hola " + u.getNombre() + ",\n\nTu ticket ha sido actualizado.\n\nDetalles: " + (detalles != null ? detalles : "(sin detalles)") + "\n\nEstado actual: " + ticket.getEstado() + "\n\nSaludos.";
        sendSimple(u.getCorreo(), subject, body);
    }

    public void notifyAssignedTechnician(Ticket ticket) {
        if (ticket == null || ticket.getAsignadoA() == null) return;
        Usuario t = ticket.getAsignadoA();
        if (t.getCorreo() == null) return;
        String subject = "Se te ha asignado un ticket: " + ticket.getTitulo();
        String body = "Hola " + t.getNombre() + ",\n\nSe te ha asignado el ticket: \nTítulo: " + ticket.getTitulo() + "\nDescripción: " + ticket.getDescripcion() + "\n\nPor favor revisa el sistema para más detalles.";
        sendSimple(t.getCorreo(), subject, body);
    }

    // Public helper to send a test email (used by dev/test endpoint)
    public boolean sendTestEmail(String to, String subject, String body) {
        try {
            sendSimple(to, subject, body);
            return true;
        } catch (Exception e) {
            System.err.println("sendTestEmail failed: " + e.getMessage());
            return false;
        }
    }
}
