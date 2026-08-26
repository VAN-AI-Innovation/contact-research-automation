package com.vanai.backend.persistence.dto;

public record ContactUpdateRequest(
        String organizationName,
        String personName,
        String department,
        String position,
        String email,
        String phone
) {
}
