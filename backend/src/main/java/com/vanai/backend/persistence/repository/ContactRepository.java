package com.vanai.backend.persistence.repository;

import com.vanai.backend.persistence.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository
        extends JpaRepository<Contact, Long> {

    List<Contact> findByCrawlSessionJobIdOrderByIdAsc(
            String jobId
    );

    long countByCrawlSessionJobId(String jobId);

    void deleteByCrawlSessionJobId(String jobId);
}
