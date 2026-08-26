package com.vanai.backend.persistence.controller;

import com.vanai.backend.persistence.dto.ContactExcludeRequest;
import com.vanai.backend.persistence.dto.ContactExcludeResponse;
import com.vanai.backend.persistence.dto.ContactResponse;
import com.vanai.backend.persistence.dto.ContactUpdateRequest;
import com.vanai.backend.persistence.service.ContactService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PatchMapping("/exclude")
    public ResponseEntity<ContactExcludeResponse> excludeContacts(
            @RequestBody ContactExcludeRequest request
    ) {
        return ResponseEntity.ok(
                contactService.excludeContacts(request)
        );
    }

    @PatchMapping("/{contactId}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable Long contactId,
            @RequestBody ContactUpdateRequest request
    ) {
        return ResponseEntity.ok(
                contactService.updateContact(contactId, request)
        );
    }
}