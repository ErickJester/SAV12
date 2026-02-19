package com.example.demo.config;

import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Paso A (temporal): puente entre el login manual por HttpSession y Spring Security.
 *
 * <p>Este filtro NO autentica credenciales; solo toma el estado existente en sesión
 * (atributo "usuario") y lo refleja en SecurityContext para proteger rutas.
 * En Paso B se debe reemplazar por autenticación real con UserDetailsService + roles.
 */
@Component
public class ManualSessionAuthenticationFilter extends OncePerRequestFilter {

    public static final String SESSION_USER_ATTRIBUTE = "usuario";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object sessionUser = session.getAttribute(SESSION_USER_ATTRIBUTE);
                if (sessionUser instanceof Usuario usuario) {
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            buildAuthorities(usuario)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> buildAuthorities(Usuario usuario) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_LOGGED_USER"));

        Rol rol = usuario.getRol();
        if (rol != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        }

        return authorities;
    }
}
