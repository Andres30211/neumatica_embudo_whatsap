package com.neumatica.embudo.whatsap.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neumatica.embudo.whatsap.dto.contact.ContactResponse;
import com.neumatica.embudo.whatsap.repository.ContactManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactManagementController {

    private final ContactManagementService contactManagementService;

    @GetMapping("/getContacts")
    public ResponseEntity<Page<ContactResponse>> getContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return ResponseEntity.ok(
                contactManagementService.getContacts(pageable)
        );
    }

    @GetMapping("/getContactById/{id}")
    public ResponseEntity<ContactResponse> getContactById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                contactManagementService.getContactById(id)
        );
    }

}
