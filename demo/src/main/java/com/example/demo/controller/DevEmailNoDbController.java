package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@RestController
public class DevEmailNoDbController {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    // Sends a test email without using repositories/DB
    @GetMapping("/dev/send-email-no-db")
    public String sendNoDb(@RequestParam String to,
                           @RequestParam(required = false) String subject,
                           @RequestParam(required = false) String body) {
        if (mailSender == null) {
            return "MAIL_SENDER_NOT_AVAILABLE";
        }
        String s = (subject == null || subject.isEmpty()) ? "Prueba de correo desde SAV12 (no-db)" : subject;
        String b = (body == null || body.isEmpty()) ? "Este es un correo de prueba enviado desde la aplicación (no-db)." : body;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(to);
            helper.setSubject(s);
            helper.setText(b, false);
            mailSender.send(message);
            return "EMAIL_SENT";
        } catch (MessagingException | MailException ex) {
            return "EMAIL_FAILED: " + ex.getMessage();
        } catch (Exception ex) {
            return "EMAIL_FAILED: " + ex.getMessage();
        }
    }
}
