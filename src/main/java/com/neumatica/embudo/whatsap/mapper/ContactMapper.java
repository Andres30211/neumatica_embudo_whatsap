package com.neumatica.embudo.whatsap.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.neumatica.embudo.whatsap.dto.webhook.ContactDto;
import com.neumatica.embudo.whatsap.entitys.Contact;

@Component
public class ContactMapper {

	public Contact toEntity(ContactDto dto){

        return Contact.builder()
                .phone(dto.getWaId())
                .metaUserId(dto.getUserId())
                .name(dto.getProfile().getName())
                .firstContact(LocalDateTime.now())
                .lastInteraction(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

    }
}
