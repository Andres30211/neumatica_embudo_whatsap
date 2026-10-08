package com.neumatica.embudo.whatsap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.neumatica.embudo.whatsap.dto.contact.ContactImportResponse;
import com.neumatica.embudo.whatsap.repository.ContactImportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactImportController {

    private final ContactImportService contactImportService;

    @PostMapping("/import")
    public ResponseEntity<ContactImportResponse> importContacts(
            @RequestParam("file") MultipartFile file) {

        ContactImportResponse response =
                contactImportService.importContacts(file);

        return ResponseEntity.ok(response);
    }
}
