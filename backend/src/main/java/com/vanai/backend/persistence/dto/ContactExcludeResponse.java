package com.vanai.backend.persistence.dto;

import java.util.List;

public record ContactExcludeResponse(
        int excludedCount,
        List<Long> contactIds
) {
}