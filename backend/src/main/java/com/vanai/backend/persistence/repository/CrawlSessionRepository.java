package com.vanai.backend.persistence.repository;

import com.vanai.backend.persistence.entity.CrawlSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CrawlSessionRepository
        extends JpaRepository<CrawlSession, Long> {

    Optional<CrawlSession> findByJobId(String jobId);

    boolean existsByJobId(String jobId);

    List<CrawlSession> findAllByOrderByCreatedAtDesc();
}
