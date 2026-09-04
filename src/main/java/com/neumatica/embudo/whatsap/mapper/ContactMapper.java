package com.neumatica.embudo.whatsap.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.neumatica.embudo.whatsap.dto.webhook.ContactDto;
import com.neumatica.embudo.whatsap.entitys.Contact;

@Component
public class ContactMapper {

    /*
     * Convierte la información disponible del webhook
     * en un Contact.
     *
     * Ningún dato proveniente de Meta se considera obligatorio,
     * salvo nuestro identificador interno generado por JPA.
     */
    public Contact toEntity(ContactDto dto) {

        LocalDateTime now = LocalDateTime.now();

        return Contact.builder()
            .phone(
                dto != null
                    ? dto.getWaId()
                    : null
            )
            .metaUserId(
                dto != null
                    ? dto.getUserId()
                    : null
            )
            .name(
                extractName(dto)
            )
            .firstContact(now)
            .lastInteraction(now)
            .createdAt(now)
            .build();
    }

    /*
     * Extrae el nombre sin asumir que profile
     * siempre viene en el webhook.
     */
    private String extractName(ContactDto dto) {

        if (dto == null ||
            dto.getProfile() == null) {

            return null;
        }

        return dto.getProfile().getName();
    }
}