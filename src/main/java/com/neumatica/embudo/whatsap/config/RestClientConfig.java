package com.neumatica.embudo.whatsap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuración central del cliente HTTP.
 */
@Configuration
public class RestClientConfig {

    /**
     * Crea el RestClient que será utilizado para
     * comunicarnos con otros microservicios.
     */
    @Bean
    public RestClient restClient() {

        return RestClient.builder()
                .build();
    }
}
