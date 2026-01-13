package com.example.demo.controller;

import com.example.demo.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.context.annotation.Profile;

@Profile("dev")
@RestController
public class DevEmailController {

    @Autowired
    private EmailService emailService;

    // Dev-only endpoint to test email sending. Use only in development.
    @GetMapping("/dev/send-email")
    public String sendTest(@RequestParam String to, @RequestParam(required = false) String subject,
                           @RequestParam(required = false) String body) {
        String s = (subject == null || subject.isEmpty()) ? "Prueba de correo desde SAV12" : subject;
        String b = (body == null || body.isEmpty()) ? "Este es un correo de prueba enviado desde la aplicación." : body;
        boolean ok = emailService.sendTestEmail(to, s, b);
        return ok ? "EMAIL_SENT" : "EMAIL_FAILED";
    }
}
