package com.vanai.backend.persistence.dto;

import com.vanai.backend.crawler.CrawlJobStatus;
import com.vanai.backend.persistence.entity.CrawlSession;

import java.time.LocalDateTime;

public record SessionResponse(
        String jobId,
        String startUrl,
        CrawlJobStatus status,
        int maxPages,
        int visitedPages,
        int collectedContacts,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SessionResponse from(CrawlSession session) {
        return new SessionResponse(
                session.getJobId(),
                session.getStartUrl(),
                session.getStatus(),
                session.getMaxPages(),
                session.getVisitedPages(),
                session.getCollectedContacts(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
