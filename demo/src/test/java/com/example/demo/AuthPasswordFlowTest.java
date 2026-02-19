package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthPasswordFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registroConCsrfGuardaHashBcrypt() throws Exception {
        mockMvc.perform(post("/registro")
                        .param("nombre", "Test User")
                        .param("correo", "test.user@example.com")
                        .param("password", "abc123")
                        .param("password2", "abc123")
                        .param("rol", "ALUMNO")
                        .param("boleta", "2021609999")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registro=success"));

        Usuario usuario = usuarioRepository.findByCorreo("test.user@example.com").orElseThrow();
        assertThat(usuario.getPasswordHash()).isNotEqualTo("abc123");
        assertThat(passwordEncoder.matches("abc123", usuario.getPasswordHash())).isTrue();
    }

    @Test
    void loginConCredencialesCorrectasRedirigeSegunRol() throws Exception {
        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setCorreo("admin.stepb@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setIdTrabajador("ADM-01");
        admin.setActivo(true);
        usuarioRepository.save(admin);

        mockMvc.perform(post("/login")
                        .param("correo", "admin.stepb@example.com")
                        .param("password", "admin123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void loginConPasswordIncorrectoRedirigeAError() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("Alumno");
        usuario.setCorreo("alumno.stepb@example.com");
        usuario.setPasswordHash(passwordEncoder.encode("correcta"));
        usuario.setRol(Rol.ALUMNO);
        usuario.setBoleta("2021601111");
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/login")
                        .param("correo", "alumno.stepb@example.com")
                        .param("password", "incorrecta")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }


    @Test
    void loginSinCsrfRegresa403() throws Exception {
        mockMvc.perform(post("/login")
                        .param("correo", "nadie@example.com")
                        .param("password", "x"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroSinCsrfRegresa403() throws Exception {
        mockMvc.perform(post("/registro")
                        .param("nombre", "Sin CSRF")
                        .param("correo", "sin.csrf@example.com")
                        .param("password", "abc123")
                        .param("password2", "abc123")
                        .param("rol", "ALUMNO")
                        .param("boleta", "2021602222"))
                .andExpect(status().isForbidden());
    }
}
