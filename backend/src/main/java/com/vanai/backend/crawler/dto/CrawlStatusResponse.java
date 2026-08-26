package com.vanai.backend.crawler.dto;

import com.vanai.backend.crawler.CrawlJob;
import com.vanai.backend.crawler.CrawlJobStatus;

public record CrawlStatusResponse(
        String jobId,
        CrawlJobStatus status,
        int visitedPages,
        int collectedContacts,
        boolean stopRequested
) {
    public static CrawlStatusResponse from(CrawlJob job) {
        return new CrawlStatusResponse(
                job.getJobId(),
                job.getStatus(),
                job.getVisitedPages(),
                job.getCollectedContacts(),
                job.isStopRequested()
        );
    }
}
