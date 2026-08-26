package com.vanai.backend.persistence.dto;

import com.vanai.backend.persistence.entity.Contact;

public record ContactResponse(
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
