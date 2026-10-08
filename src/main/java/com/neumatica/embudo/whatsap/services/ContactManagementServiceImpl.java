package com.neumatica.embudo.whatsap.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neumatica.embudo.whatsap.dto.contact.ContactResponse;
import com.neumatica.embudo.whatsap.entitys.Contact;
import com.neumatica.embudo.whatsap.repository.ContactManagementService;
import com.neumatica.embudo.whatsap.repository.ContactRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactManagementServiceImpl implements ContactManagementService {

    private final ContactRepository contactRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ContactResponse> getContacts(Pageable pageable) {

        return contactRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactResponse getContactById(UUID id) {

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Contacto no encontrado: " + id)
                );

        return toResponse(contact);
    }


    private ContactResponse toResponse(Contact contact) {

        String registrationStatus =
                contact.getRegistrationStep() != null
                        && contact.getRegistrationStep().name().equals("COMPLETED")
                        ? "COMPLETO"
                        : "INCOMPLETO";

        return ContactResponse.builder()
                .id(contact.getId())
                .name(contact.getName())
                .phone(contact.getPhone())
                .email(contact.getEmail())
                .company(contact.getCompany())
                .registrationStep(contact.getRegistrationStep())
                .registrationStatus(registrationStatus)
                .firstContact(contact.getFirstContact())
                .lastInteraction(contact.getLastInteraction())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}
