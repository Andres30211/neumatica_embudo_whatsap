package com.neumatica.embudo.whatsap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
                 * ====================================================
                 * CORS
                 * ====================================================
                 */
                .cors(Customizer.withDefaults())


                /*
                 * ====================================================
                 * CSRF
                 * ====================================================
                 */
                .csrf(csrf -> csrf.disable())


                /*
                 * ====================================================
                 * SESIONES
                 * ====================================================
                 *
                 * API completamente stateless.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                /*
                 * ====================================================
                 * AUTORIZACIÓN
                 * ====================================================
                 */
                .authorizeHttpRequests(auth -> auth

                        /*
                         * ------------------------------------------------
                         * WEBHOOK WHATSAPP / META
                         * ------------------------------------------------
                         */
                        .requestMatchers(
                                "/webhook/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * WEBSOCKET
                         * ------------------------------------------------
                         *
                         * IMPORTANTE:
                         *
                         * El handshake inicial del WebSocket debe poder
                         * realizarse sin JWT HTTP.
                         *
                         * La autenticación STOMP puede realizarse
                         * posteriormente en el frame CONNECT.
                         */
                        .requestMatchers(
                                "/wss",
                                "/wss/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * CORS / PREFLIGHT
                         * ------------------------------------------------
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * REST API
                         * ------------------------------------------------
                         *
                         * Las conversaciones siguen protegidas mediante
                         * JWT.
                         */
                        .requestMatchers(
                                "/api/conversations/**"
                        ).authenticated()


                        /*
                         * ------------------------------------------------
                         * RESTO DE LA APLICACIÓN
                         * ------------------------------------------------
                         */
                        .anyRequest().authenticated()
                )


                /*
                 * ====================================================
                 * JWT RESOURCE SERVER
                 * ====================================================
                 */
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(
                                Customizer.withDefaults()
                        )
                );

        return http.build();
    }
}