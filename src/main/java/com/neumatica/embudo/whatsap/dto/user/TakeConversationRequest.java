package com.neumatica.embudo.whatsap.dto.user;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos enviados por Angular cuando un vendedor
 * quiere tomar una conversación.
 *
 * NOTA:
 *
 * Por ahora enviamos userId porque todavía no tenemos
 * Spring Security integrado en el microservicio del embudo.
 *
 * Cuando integremos JWT, este DTO ya no necesitará
 * userId.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TakeConversationRequest {

    private UUID userId;
}
