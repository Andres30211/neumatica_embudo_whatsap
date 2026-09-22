package com.neumatica.embudo.whatsap.dto.user;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO utilizado para recibir información del User
 * desde el microservicio de seguridad.
 *
 * NO es una entidad JPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private UUID id;

    private String name;

    private String email;

    private boolean enabled;

    private boolean accountNonLocked;
}
