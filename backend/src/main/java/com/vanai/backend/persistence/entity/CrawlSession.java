package com.vanai.backend.persistence.entity;

import com.vanai.backend.crawler.CrawlJobStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "crawl_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crawl_session_job_id",
                        columnNames = "job_id"
                )
        }
)
public class CrawlSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true, length = 100)
    private String jobId;

    @Column(name = "start_url", nullable = false, length = 2000)
    private String startUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CrawlJobStatus status;

    @Column(name = "max_pages", nullable = false)
    private int maxPages;

    @Column(name = "visited_pages", nullable = false)
    private int visitedPages;

    @Column(name = "collected_contacts", nullable = false)
    private int collectedContacts;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "crawlSession",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Contact> contacts = new ArrayList<>();

    protected CrawlSession() {
    }

    public CrawlSession(
            String jobId,
            String startUrl,
            CrawlJobStatus status,
            int maxPages
    ) {
        this.jobId = jobId;
        this.startUrl = startUrl;
        this.status = status;
        this.maxPages = maxPages;
        this.visitedPages = 0;
        this.collectedContacts = 0;
        this.startedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (startedAt == null) {
            startedAt = now;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void updateProgress(
            int visitedPages,
            int collectedContacts
    ) {
        this.visitedPages = visitedPages;
        this.collectedContacts = collectedContacts;
    }

    public void changeStatus(CrawlJobStatus status) {
        this.status = status;

        if (status == CrawlJobStatus.COMPLETED
                || status == CrawlJobStatus.STOPPED
                || status == CrawlJobStatus.FAILED) {

            this.completedAt = LocalDateTime.now();
        }
    }

    public void addContact(Contact contact) {
        contacts.add(contact);
        contact.setCrawlSession(this);
    }

    public void clearContacts() {
        contacts.clear();
    }

    public Long getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public CrawlJobStatus getStatus() {
        return status;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public int getVisitedPages() {
        return visitedPages;
    }

    public int getCollectedContacts() {
        return collectedContacts;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Contact> getContacts() {
        return contacts;
    }
}
