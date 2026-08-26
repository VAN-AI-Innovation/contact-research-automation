package com.vanai.backend.persistence.dto;

import java.util.List;

public record ContactExcludeRequest(
        List<Long> contactIds
) {
}