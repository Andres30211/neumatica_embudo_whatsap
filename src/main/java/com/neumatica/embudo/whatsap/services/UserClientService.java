package com.neumatica.embudo.whatsap.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.neumatica.embudo.whatsap.dto.user.UserResponseDto;

/**
 * Servicio responsable de comunicarse con el
 * microservicio de seguridad.
 *
 * Este servicio NO utiliza entidades JPA del Security Service.
 */
@Service
public class UserClientService {

    private final RestClient restClient;

    /*
     * URL base del microservicio de seguridad.
     */
    private final String securityServiceUrl;

    public UserClientService(
            RestClient restClient,
            @Value("${security.service.url}") String securityServiceUrl
    ) {

        this.restClient = restClient;
        this.securityServiceUrl = securityServiceUrl;
    }

    /**
     * Consulta un usuario por UUID en el Security Service.
     *
     * Ejemplo:
     *
     * GET https://security-service-neumatica.onrender.com/api/users/{id}
     */
    public UserResponseDto findById(UUID userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "El userId no puede ser null."
            );
        }

        return restClient
                .get()
                .uri(
                    securityServiceUrl
                        + "/api/users/"
                        + userId
                )
                .retrieve()

                /*
                 * Si Security Service devuelve 404,
                 * transformamos la respuesta en una excepción
                 * controlada.
                 */
                .onStatus(
                    HttpStatusCode::is4xxClientError,
                    (request, response) -> {
                        throw new RuntimeException(
                            "Usuario no encontrado en Security Service."
                        );
                    }
                )

                /*
                 * Errores 5xx del microservicio de seguridad.
                 */
                .onStatus(
                    HttpStatusCode::is5xxServerError,
                    (request, response) -> {
                        throw new RuntimeException(
                            "Security Service no está disponible."
                        );
                    }
                )

                .body(UserResponseDto.class);
    }
}
