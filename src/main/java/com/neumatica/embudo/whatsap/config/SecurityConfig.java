package com.neumatica.embudo.whatsap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                 * Permitimos que Angular pueda consumir
                 * nuestro backend.
                 */
                .cors(Customizer.withDefaults())


                /*
                 * ====================================================
                 * CSRF
                 * ====================================================
                 *
                 * No utilizamos sesiones ni formularios.
                 * Trabajamos con JWT.
                 */
                .csrf(csrf -> csrf.disable())


                /*
                 * ====================================================
                 * SESIONES
                 * ====================================================
                 *
                 * La API es stateless.
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
                         * Meta necesita poder llamar a nuestro webhook
                         * sin enviar el JWT de un vendedor.
                         */
                        .requestMatchers(
                                "/webhook/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * WEBSOCKET
                         * ------------------------------------------------
                         *
                         * El handshake inicial del WebSocket no lo
                         * vamos a bloquear con HTTP Basic/JWT.
                         *
                         * Posteriormente podremos autenticar el usuario
                         * mediante STOMP CONNECT.
                         */
                        .requestMatchers(
                                "/wss",
                                "/wss/**"
                        ).permitAll()


                        /*
                         * ------------------------------------------------
                         * OPTIONS
                         * ------------------------------------------------
                         *
                         * Necesario para los preflight CORS de Angular.
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
                         * Estas rutas sí requieren JWT.
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
                 * OAUTH2 RESOURCE SERVER / JWT
                 * ====================================================
                 *
                 * Angular envía:
                 *
                 * Authorization: Bearer <JWT>
                 *
                 * Spring valida ese JWT.
                 */
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(
                                Customizer.withDefaults()
                        )
                );


        return http.build();
    }


    /**
     * ================================================================
     * CONFIGURACIÓN CORS
     * ================================================================
     *
     * Angular:
     *
     * http://localhost:4200
     *
     * Producción:
     *
     * https://neumatica-crm.netlify.app
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * Orígenes permitidos.
         */
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:4200",
                        "https://neumatica-crm.netlify.app"
                )
        );

        /*
         * Métodos HTTP permitidos.
         */
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        /*
         * Headers que Angular puede enviar.
         */
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        /*
         * Headers que el navegador puede leer.
         */
        configuration.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Disposition"
                )
        );

        /*
         * Como estamos utilizando JWT y no cookies,
         * no necesitamos credenciales de navegador.
         */
        configuration.setAllowCredentials(false);

        /*
         * Duración del resultado del preflight.
         */
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}