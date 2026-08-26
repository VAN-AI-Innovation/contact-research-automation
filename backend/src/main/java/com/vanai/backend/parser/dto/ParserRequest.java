package com.vanai.backend.parser.dto;

import jakarta.validation.constraints.NotBlank;

public record ParserRequest(
        @NotBlank(message = "URL은 필수입니다.")
        String url
) {
}
