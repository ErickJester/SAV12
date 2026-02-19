package com.example.demo.security;

import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        return User.withUsername(usuario.getCorreo())
                .password(usuario.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())))
                .disabled(Boolean.FALSE.equals(usuario.getActivo()))
                .build();
    }
}
