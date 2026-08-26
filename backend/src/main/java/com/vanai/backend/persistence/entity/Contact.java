package com.vanai.backend.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crawl_session_id", nullable = false)
    private CrawlSession crawlSession;

    @Column(name = "organization_name", length = 500)
    private String organizationName;

    @Column(name = "person_name", length = 200)
    private String personName;

    @Column(length = 300)
    private String department;

    @Column(length = 200)
    private String position;

    @Column(length = 500)
    private String email;

    @Column(length = 100)
    private String phone;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected Contact() {
    }

    public Contact(
            String organizationName,
            String personName,
            String department,
            String position,
            String email,
            String phone,
            String sourceUrl
    ) {
        this.organizationName = organizationName;
        this.personName = personName;
        this.department = department;
        this.position = position;
        this.email = email;
        this.phone = phone;
        this.sourceUrl = sourceUrl;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setCrawlSession(CrawlSession crawlSession) {
        this.crawlSession = crawlSession;
    }

    public void update(
            String organizationName,
            String personName,
            String department,
            String position,
            String email,
            String phone
    ) {
        if (organizationName != null) {
            this.organizationName = normalize(organizationName);
        }

        if (personName != null) {
            this.personName = normalize(personName);
        }

        if (department != null) {
            this.department = normalize(department);
        }

        if (position != null) {
            this.position = normalize(position);
        }

        if (email != null) {
            this.email = normalize(email);
        }

        if (phone != null) {
            this.phone = normalize(phone);
        }
    }

    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    public Long getId() {
        return id;
    }

    public CrawlSession getCrawlSession() {
        return crawlSession;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getPersonName() {
        return personName;
    }

    public String getDepartment() {
        return department;
    }

    public String getPosition() {
        return position;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}