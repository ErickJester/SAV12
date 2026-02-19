package com.example.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.config.ManualSessionAuthenticationFilter;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecuritySmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminRouteSinSesionRedirigeALogin() throws Exception {
        mockMvc.perform(get("/admin/reportes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminRouteConSesionManualNoRedirigeALogin() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("Admin Test");
        usuario.setCorreo("admin.test@example.com");
        usuario.setRol(Rol.ADMIN);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(ManualSessionAuthenticationFilter.SESSION_USER_ATTRIBUTE, usuario);

        mockMvc.perform(get("/admin/reportes").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void recursoEstaticoEsPublico() throws Exception {
        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());
    }
}
