package com.neumatica.embudo.whatsap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                /*
                 * API REST.
                 *
                 * No utilizamos CSRF porque
                 * trabajaremos con JWT.
                 */
                .csrf(csrf -> csrf.disable())


                /*
                 * No utilizamos sesiones.
                 *
                 * Cada petición debe traer
                 * su JWT.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                /*
                 * Autorización.
                 */
                .authorizeHttpRequests(auth -> auth

                        /*
                         * Endpoints públicos.
                         *
                         * Podemos agregar aquí
                         * health checks posteriormente.
                         */
                        .requestMatchers(
                                "/webhook/**",
                                "/webhook/messages/**",
                                "/api/conversations/**"
                        ).permitAll()


                        /*
                         * Todo lo demás requiere
                         * autenticación.
                         */
                        .anyRequest().authenticated()
                )


                /*
                 * Configuramos Spring Security
                 * para recibir JWT Bearer Token.
                 */
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(
                                jwt -> {}
                        )
                );


        return http.build();
    }
}

