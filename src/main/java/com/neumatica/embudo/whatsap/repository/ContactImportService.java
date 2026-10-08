package com.neumatica.embudo.whatsap.repository;

import org.springframework.web.multipart.MultipartFile;

import com.neumatica.embudo.whatsap.dto.contact.ContactImportResponse;

public interface ContactImportService {

    ContactImportResponse importContacts(MultipartFile file);
}
