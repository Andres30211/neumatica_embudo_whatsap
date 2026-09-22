package com.neumatica.embudo.whatsap.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * Dominio de nuestro frontend Angular
         *
         * IMPORTANTE:
         * No colocar "/" al final.
         */
        configuration.setAllowedOrigins(
                List.of(
                        "https://neumatica-crm.netlify.app",
                        "http://localhost:4200"
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
         *
         * Authorization es importante porque
         * estamos utilizando JWT.
         */
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        /*
         * Permitimos que el navegador envíe
         * credenciales/autorización cuando corresponda.
         *
         * IMPORTANTE:
         * Esto funciona porque estamos utilizando
         * un origen específico y NO "*".
         */
        configuration.setAllowCredentials(true);

        /*
         * Tiempo durante el cual el navegador
         * puede almacenar la respuesta del preflight.
         */
        configuration.setMaxAge(3600L);

        /*
         * Aplicar esta configuración a todos
         * los endpoints.
         */
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}
