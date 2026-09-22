package com.neumatica.embudo.whatsap.config;

import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    /*
     * Ruta donde está almacenada la clave pública.
     *
     * IMPORTANTE:
     * Este microservicio solamente necesita
     * la clave pública.
     */
    @Value("${app.jwt.public-key}")
    private Resource publicKeyResource;


    /*
     * Cargamos la clave pública RSA.
     */
    @Bean
    public RSAPublicKey publicKey() throws Exception {

        try (
            InputStream inputStream =
                    publicKeyResource.getInputStream()
        ) {

            return RsaKeyConverters
                    .x509()
                    .convert(inputStream);
        }
    }


    /*
     * JwtDecoder se encargará de validar
     * la firma del JWT.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            RSAPublicKey publicKey
    ) {

        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();
    }
}

