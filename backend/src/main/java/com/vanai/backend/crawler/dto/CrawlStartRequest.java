package com.vanai.backend.crawler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CrawlStartRequest(
        @NotBlank(message = "시작 URL은 필수입니다.")
        String url,

        @Min(value = 1, message = "maxPages는 최소 1이어야 합니다.")
        @Max(value = 100, message = "maxPages는 최대 100까지 가능합니다.")
        Integer maxPages
) {
    public int resolvedMaxPages() {
        return maxPages == null ? 20 : maxPages;
    }
}
