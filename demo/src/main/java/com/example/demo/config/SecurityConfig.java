package com.example.demo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final ManualSessionAuthenticationFilter manualSessionAuthenticationFilter;

    public SecurityConfig(ManualSessionAuthenticationFilter manualSessionAuthenticationFilter) {
        this.manualSessionAuthenticationFilter = manualSessionAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Paso A (temporal): solo protección de rutas con sesión manual.
        // Paso B reemplazará esto por autenticación completa (UserDetailsService + formLogin + roles formales).
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/registro",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/admin/**", "/tecnico/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> response.sendRedirect("/login"))
                )
                // Step A: CSRF disabled temporalmente. Step B lo reactiva y se agregan tokens a formularios.
                .csrf(csrf -> csrf.disable())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(withDefaults())
                .addFilterBefore(manualSessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
