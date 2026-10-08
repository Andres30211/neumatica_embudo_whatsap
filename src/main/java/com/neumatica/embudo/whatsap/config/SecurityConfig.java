package com.neumatica.embudo.whatsap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
                         * WEBHOOK WHATSAPP / META
                         */
                        .requestMatchers(
                                "/webhook/**"
                        ).permitAll()

                        /*
                         * WEBSOCKET
                         */
                        .requestMatchers(
                                "/wss",
                                "/wss/**"
                        ).permitAll()

                        /*
                         * CORS / PREFLIGHT
                         */
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        /*
                         * REST API
                         */
                        .requestMatchers(
                                "/api/conversations/**",
                                "/api/contacts/getContacts",
                                "/api/contacts/getContactById/**"
                        ).authenticated()

                        /*
                         * ADMIN
                         */
                        .requestMatchers(
                                "/api/contacts/import",
                                "/webhook/delete/**"
                        ).hasRole("ADMIN")

                        /*
                         * RESTO DE LA APLICACIÓN
                         */
                        .anyRequest().authenticated()
                )

                /*
                 * ====================================================
                 * JWT RESOURCE SERVER
                 * ====================================================
                 */
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .jwt(jwt -> jwt
                                        .jwtAuthenticationConverter(
                                                jwtAuthenticationConverter()
                                        )
                                )
                );

        return http.build();
    }


    /**
     * Convierte el claim "roles" del JWT en authorities
     * de Spring Security.
     *
     * JWT:
     *
     * "roles": [
     *     "ROLE_ADMIN"
     * ]
     *
     * Spring:
     *
     * ROLE_ADMIN
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        // Claim personalizado de nuestro JWT
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        // El JWT ya trae ROLE_ADMIN, por lo tanto NO agregamos
        // otro prefijo.
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                grantedAuthoritiesConverter
        );

        return jwtAuthenticationConverter;
    }
}