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
                 *
                 * Spring utilizará el bean corsConfigurationSource()
                 * que ya tienes definido en CorsConfig.java.
                 */
                .cors(Customizer.withDefaults())


                /*
                 * ====================================================
                 * CSRF
                 * ====================================================
                 *
                 * No utilizamos CSRF porque nuestra API trabaja
                 * con JWT y no con sesiones de navegador.
                 */
                .csrf(csrf -> csrf.disable())


                /*
                 * ====================================================
                 * SESIONES
                 * ====================================================
                 *
                 * La aplicación es Stateless.
                 *
                 * Cada petición autenticada debe traer su JWT.
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
                         * WEBHOOK DE WHATSAPP / META
                         * ------------------------------------------------
                         *
                         * Meta necesita poder llamar a estos endpoints
                         * sin tener el JWT de un vendedor.
                         */
                        .requestMatchers(
                                "/webhook/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * WEBSOCKET
                         * ------------------------------------------------
                         *
                         * Permitimos el handshake inicial.
                         *
                         * Posteriormente podremos autenticar el usuario
                         * mediante STOMP.
                         */
                        .requestMatchers(
                                "/wss",
                                "/wss/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * PETICIONES OPTIONS
                         * ------------------------------------------------
                         *
                         * Necesarias para CORS/preflight de Angular.
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * CONVERSACIONES
                         * ------------------------------------------------
                         *
                         * Estas rutas SÍ requieren autenticación JWT.
                         *
                         * Ejemplo:
                         *
                         * POST /api/conversations/{id}/take
                         *
                         * El controlador obtiene el usuario mediante:
                         *
                         * @AuthenticationPrincipal Jwt jwt
                         */
                        .requestMatchers(
                                "/api/conversations/**"
                        ).authenticated()


                        /*
                         * ------------------------------------------------
                         * TODO LO DEMÁS
                         * ------------------------------------------------
                         */
                        .anyRequest().authenticated()
                )


                /*
                 * ====================================================
                 * JWT RESOURCE SERVER
                 * ====================================================
                 *
                 * Angular envía:
                 *
                 * Authorization: Bearer <JWT>
                 *
                 * Spring Security valida el JWT.
                 */
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(
                                Customizer.withDefaults()
                        )
                );

        return http.build();
    }
}