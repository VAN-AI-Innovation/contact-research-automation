package com.vanai.backend.crawler;

import com.vanai.backend.parser.dto.ParserResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CrawlJob {

    private final String jobId;
    private final String startUrl;
    private final int maxPages;
    private final LocalDateTime createdAt;

    private volatile CrawlJobStatus status;
    private volatile boolean stopRequested;
    private volatile int visitedPages;

    private final List<ParserResponse> contacts = new ArrayList<>();

    public CrawlJob(String jobId, String startUrl, int maxPages) {
        this.jobId = jobId;
        this.startUrl = startUrl;
        this.maxPages = maxPages;
        this.createdAt = LocalDateTime.now();
        this.status = CrawlJobStatus.PENDING;
        this.stopRequested = false;
        this.visitedPages = 0;
    }

    public String getJobId() {
        return jobId;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CrawlJobStatus getStatus() {
        return status;
    }

    public void setStatus(CrawlJobStatus status) {
        this.status = status;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public void requestStop() {
        this.stopRequested = true;
    }

    public int getVisitedPages() {
        return visitedPages;
    }

    public void incrementVisitedPages() {
        this.visitedPages++;
    }

    public void addContact(ParserResponse contact) {
        synchronized (contacts) {
            contacts.add(contact);
        }
    }

    public List<ParserResponse> getContacts() {
        synchronized (contacts) {
            return new ArrayList<>(contacts);
        }
    }

    public int getCollectedContacts() {
        synchronized (contacts) {
            return contacts.size();
        }
    }

    public void replaceContacts(List<ParserResponse> newContacts) {
        synchronized (contacts) {
            contacts.clear();
            contacts.addAll(newContacts);
        }
    }

}