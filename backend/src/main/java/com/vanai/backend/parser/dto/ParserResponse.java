package com.vanai.backend.parser.dto;

public record ParserResponse(
        String organizationName,
        String personName,
        String department,
        String position,
        String email,
        String phone,
        String sourceUrl
) {
}
