package com.neumatica.embudo.whatsap.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.neumatica.embudo.whatsap.dto.contact.ContactResponse;

public interface ContactManagementService {

    Page<ContactResponse> getContacts(Pageable pageable);

    ContactResponse getContactById(UUID id);
}
