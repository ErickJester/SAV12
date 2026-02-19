package com.example.demo.security;

import com.example.demo.DTO.TicketDTO;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Ticket;
import com.example.demo.entity.Usuario;
import com.example.demo.service.CatalogoService;
import com.example.demo.service.ComentarioService;
import com.example.demo.service.ExportService;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.ReporteService;
import com.example.demo.service.TicketService;
import com.example.demo.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityStepBTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UsuarioService usuarioService;
    @MockBean private ReporteService reporteService;
    @MockBean private TicketService ticketService;
    @MockBean private CatalogoService catalogoService;
    @MockBean private ComentarioService comentarioService;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private ExportService exportService;

    @BeforeEach
    void setupMocks() {
        // Usuario ADMIN simulado
        Usuario admin = new Usuario();
        admin.setId(1L);
        admin.setCorreo("admin@test.com");
        admin.setNombre("Admin Test");
        admin.setRol(Rol.ADMIN);

        // Usuario ALUMNO simulado
        Usuario alumno = new Usuario();
        alumno.setId(2L);
        alumno.setCorreo("alumno@test.com");
        alumno.setNombre("Alumno Test");
        alumno.setRol(Rol.ALUMNO);

        when(usuarioService.obtenerPorCorreo("admin@test.com")).thenReturn(admin);
        when(usuarioService.obtenerPorCorreo("alumno@test.com")).thenReturn(alumno);

        // admin/panel usa "reporte"
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("totalTickets", 0);
        reporte.put("ticketsAbiertos", 0);
        reporte.put("ticketsEnProceso", 0);
        reporte.put("ticketsResueltos", 0);
        reporte.put("ticketsCerrados", 0);
        reporte.put("ticketsCancelados", 0);

        when(reporteService.generarReporteGeneral()).thenReturn(reporte);

        // crear-ticket: el método NO es void, regresa Ticket
        when(ticketService.crearTicket(any(TicketDTO.class), any(Usuario.class)))
                .thenReturn(new Ticket());
    }

    @Test
    void noAutenticado_noPuedeEntrarAdmin_redirigeALogin() throws Exception {
        mockMvc.perform(get("/admin/panel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void alumno_autenticado_noPuedeEntrarAdmin_403_o_accessDenied() throws Exception {
        mockMvc.perform(get("/admin/panel")
                        .with(user("alumno@test.com").roles("ALUMNO")))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    String loc = result.getResponse().getHeader("Location");

                    boolean ok = (s == 403)
                            || (s >= 300 && s < 400 && loc != null && loc.endsWith("/403"));

                    if (!ok) {
                        throw new AssertionError("Esperaba 403 o redirect a /403, pero fue status="
                                + s + " Location=" + loc);
                    }
                });
    }

    @Test
    void admin_autenticado_siPuedeEntrarAdmin_200() throws Exception {
        mockMvc.perform(get("/admin/panel")
                        .with(user("admin@test.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/panel"));
    }

    @Test
    void logout_funciona_postConCsrf_redirigeALoginLogoutTrue() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(user("alumno@test.com").roles("ALUMNO"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    void postSensible_sinCsrf_da403_y_conCsrf_pasa() throws Exception {
        // SIN CSRF => 403
        mockMvc.perform(post("/usuario/crear-ticket")
                        .with(user("alumno@test.com").roles("ALUMNO"))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("titulo", "X")
                        .param("descripcion", "Y"))
                .andExpect(status().isForbidden());

        // CON CSRF => pasa
        mockMvc.perform(post("/usuario/crear-ticket")
                        .with(user("alumno@test.com").roles("ALUMNO"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("titulo", "X")
                        .param("descripcion", "Y"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuario/mis-tickets?success=created"));
    }
}