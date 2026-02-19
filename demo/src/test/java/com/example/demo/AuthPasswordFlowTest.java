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
    void registroGuardaHuellaBcrypt() throws Exception {
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
        assertThat(usuario.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("abc123", usuario.getPasswordHash())).isTrue();
    }

    @Test
    void loginMigraPasswordLegacyAHash() throws Exception {
        Usuario legacy = new Usuario();
        legacy.setNombre("Legacy User");
        legacy.setCorreo("legacy@example.com");
        legacy.setPasswordHash("legacy123");
        legacy.setRol(Rol.ALUMNO);
        legacy.setBoleta("2021608888");
        legacy.setActivo(true);
        usuarioRepository.save(legacy);

        mockMvc.perform(post("/login")
                        .param("correo", "legacy@example.com")
                        .param("password", "legacy123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuario/panel"));

        Usuario migrated = usuarioRepository.findByCorreo("legacy@example.com").orElseThrow();
        assertThat(migrated.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches("legacy123", migrated.getPasswordHash())).isTrue();
    }
}
