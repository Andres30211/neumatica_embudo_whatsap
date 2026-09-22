package com.neumatica.embudo.whatsap.services;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.neumatica.embudo.whatsap.dto.user.UserResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserClientService {

    private final RestClient restClient;

    @Value("${security.service.url}")
    private String securityServiceUrl;


    public UserResponseDto findById(
            UUID userId,
            String accessToken
    ) {
        return restClient.get()
                .uri(
                        securityServiceUrl + "/api/users/{id}",
                        userId
                )
                .header(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .retrieve()
                .body(UserResponseDto.class);
    }
}