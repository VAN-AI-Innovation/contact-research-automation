package com.vanai.backend.persistence.dto;

import com.vanai.backend.persistence.entity.Contact;

public record ContactResponse(
        Long id,
        String organizationName,
        String personName,
        String department,
        String position,
        String email,
        String phone,
        String sourceUrl
) {

    public static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getOrganizationName(),
                contact.getPersonName(),
                contact.getDepartment(),
                contact.getPosition(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getSourceUrl()
        );
    }
}
