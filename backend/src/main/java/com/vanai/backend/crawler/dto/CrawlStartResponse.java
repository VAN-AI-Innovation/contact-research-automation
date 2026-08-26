package com.vanai.backend.crawler.dto;

import com.vanai.backend.crawler.CrawlJobStatus;

public record CrawlStartResponse(
        String jobId,
        CrawlJobStatus status
) {
}
