package com.example.demo.service;

import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario obtenerPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo).orElse(null);
    }

    public boolean existeCorreo(String correo) {
        return usuarioRepository.existsByCorreo(correo);
    }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public List<Usuario> obtenerTodosTecnicos() {
        return usuarioRepository.findByRol(Rol.TECNICO);
    }

    public List<Usuario> obtenerUsuariosAsignables() {
        return usuarioRepository.findByRolInAndActivoTrue(Arrays.asList(Rol.TECNICO, Rol.ADMIN));
    }

    public List<Usuario> obtenerTodosUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario guardarUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void cambiarEstadoUsuario(Long id, Boolean activo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    public boolean existeBoleta(String boleta) {
        return usuarioRepository.existsByBoleta(boleta);
    }

    public boolean existeIdTrabajador(String idTrabajador) {
        return usuarioRepository.existsByIdTrabajador(idTrabajador);
    }

}
