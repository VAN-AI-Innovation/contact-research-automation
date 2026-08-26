package com.vanai.backend.crawler.sse.dto;

import com.vanai.backend.crawler.CrawlJob;
import com.vanai.backend.crawler.CrawlJobStatus;

public record CrawlProgressEvent(
        String jobId,
        CrawlJobStatus status,
        int visitedPages,
        int maxPages,
        int collectedContacts,
        int progress
) {

    public static CrawlProgressEvent from(CrawlJob job) {

        int progress;

        if (job.getStatus() == CrawlJobStatus.COMPLETED) {
            progress = 100;
        } else if (job.getMaxPages() <= 0) {
            progress = 0;
        } else {
            progress = (int) Math.round(
                    (job.getVisitedPages() * 100.0)
                            / job.getMaxPages()
            );

            progress = Math.max(
                    0,
                    Math.min(progress, 100)
            );
        }

        return new CrawlProgressEvent(
                job.getJobId(),
                job.getStatus(),
                job.getVisitedPages(),
                job.getMaxPages(),
                job.getCollectedContacts(),
                progress
        );
    }
}
