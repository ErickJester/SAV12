package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.context.annotation.Profile;

import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Profile("dev")
@RestController
public class DevDebugController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/dev/users")
    public List<String> listUserEmails() {
        try {
            List<Usuario> users = usuarioRepository.findAll();
            return users.stream().map(u -> u.getCorreo()).collect(Collectors.toList());
        } catch (DataAccessException dae) {
            return List.of("DB_ERROR: " + dae.getMessage());
        }
    }
}
