package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;

@Testcontainers
class UsuarioPersistenceRestartTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("sav12")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void usuarioPersisteTrasReinicioDeServidor() {
        String correo = "persist-" + UUID.randomUUID() + "@sav12.test";
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", MYSQL.getJdbcUrl());
        properties.put("spring.datasource.username", MYSQL.getUsername());
        properties.put("spring.datasource.password", MYSQL.getPassword());
        properties.put("spring.jpa.hibernate.ddl-auto", "update");
        properties.put("spring.jpa.show-sql", "false");
        properties.put("spring.main.web-application-type", "none");

        try (ConfigurableApplicationContext firstContext =
                     new SpringApplicationBuilder(DemoApplication.class)
                             .properties(properties)
                             .run()) {
            UsuarioRepository usuarioRepository = firstContext.getBean(UsuarioRepository.class);
            Usuario usuario = new Usuario();
            usuario.setNombre("Usuario Persistente");
            usuario.setCorreo(correo);
            usuario.setPassword("secret");
            usuario.setRol(Rol.USUARIO);
            usuarioRepository.saveAndFlush(usuario);
        }

        try (ConfigurableApplicationContext secondContext =
                     new SpringApplicationBuilder(DemoApplication.class)
                             .properties(properties)
                             .run()) {
            UsuarioRepository usuarioRepository = secondContext.getBean(UsuarioRepository.class);
            assertThat(usuarioRepository.findByCorreo(correo)).isPresent();
        }
    }
}
