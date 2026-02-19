package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecuritySmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminRouteSinLoginRedirigeALogin() throws Exception {
        mockMvc.perform(get("/admin/reportes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void adminRouteConTecnicoRegresa403() throws Exception {
        Usuario tecnico = new Usuario();
        tecnico.setNombre("Tecnico");
        tecnico.setCorreo("tecnico.stepb@example.com");
        tecnico.setPasswordHash(passwordEncoder.encode("tec123"));
        tecnico.setRol(Rol.TECNICO);
        tecnico.setIdTrabajador("TEC-01");
        tecnico.setActivo(true);
        usuarioRepository.save(tecnico);

        MockHttpSession session = loginAndGetSession("tecnico.stepb@example.com", "tec123");

        mockMvc.perform(get("/admin/reportes").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRouteConAdminRegresa200() throws Exception {
        Usuario admin = new Usuario();
        admin.setNombre("Admin Dos");
        admin.setCorreo("admin2.stepb@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setIdTrabajador("ADM-02");
        admin.setActivo(true);
        usuarioRepository.save(admin);

        MockHttpSession session = loginAndGetSession("admin2.stepb@example.com", "admin123");

        mockMvc.perform(get("/admin/reportes").session(session))
                .andExpect(status().isOk());
    }


    @Test
    void adminRouteConAlumnoRegresa403() throws Exception {
        mockMvc.perform(get("/admin/reportes")
                        .with(user("alumno.stepb@example.com").roles("ALUMNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutPorPostConCsrfRedirigeALoginLogoutTrue() throws Exception {

        Usuario admin = new Usuario();
        admin.setNombre("Admin Logout");
        admin.setCorreo("admin.logout@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setIdTrabajador("ADM-LOG");
        admin.setActivo(true);
        usuarioRepository.save(admin);

        MockHttpSession session = loginAndGetSession("admin.logout@example.com", "admin123");

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    void postLevantarReporteSinCsrfDa403YConCsrfPasa() throws Exception {
        Usuario admin = new Usuario();
        admin.setNombre("Admin Reporte");
        admin.setCorreo("admin.reporte@example.com");
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRol(Rol.ADMIN);
        admin.setIdTrabajador("ADM-REP");
        admin.setActivo(true);
        usuarioRepository.save(admin);

        MockHttpSession session = loginAndGetSession("admin.reporte@example.com", "admin123");

        mockMvc.perform(post("/levantar-reporte")
                        .session(session)
                        .param("boleta", "2021609999")
                        .param("salon", "A101")
                        .param("fecha", "2026-02-19")
                        .param("hora", "10:30")
                        .param("titulo", "Prueba")
                        .param("descripcion", "Descripcion de prueba")
                        .param("prioridad", "MEDIA"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/levantar-reporte")
                        .session(session)
                        .param("boleta", "2021609999")
                        .param("salon", "A101")
                        .param("fecha", "2026-02-19")
                        .param("hora", "10:30")
                        .param("titulo", "Prueba")
                        .param("descripcion", "Descripcion de prueba")
                        .param("prioridad", "MEDIA")
                        .with(csrf()))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    private MockHttpSession loginAndGetSession(String correo, String password) throws Exception {
        MvcResult login = mockMvc.perform(post("/login")
                        .param("correo", correo)
                        .param("password", password)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
