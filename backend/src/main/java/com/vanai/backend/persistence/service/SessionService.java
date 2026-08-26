package com.vanai.backend.persistence.service;

import com.vanai.backend.crawler.CrawlJobStatus;
import com.vanai.backend.parser.dto.ParserResponse;
import com.vanai.backend.persistence.dto.ContactResponse;
import com.vanai.backend.persistence.dto.SessionResponse;
import com.vanai.backend.persistence.entity.Contact;
import com.vanai.backend.persistence.entity.CrawlSession;
import com.vanai.backend.persistence.repository.ContactRepository;
import com.vanai.backend.persistence.repository.CrawlSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SessionService {

    private final CrawlSessionRepository crawlSessionRepository;
    private final ContactRepository contactRepository;

    public SessionService(
            CrawlSessionRepository crawlSessionRepository,
            ContactRepository contactRepository
    ) {
        this.crawlSessionRepository = crawlSessionRepository;
        this.contactRepository = contactRepository;
    }

    @Transactional
    public void createSession(
            String jobId,
            String startUrl,
            int maxPages
    ) {

        if (crawlSessionRepository.existsByJobId(jobId)) {
            return;
        }

        CrawlSession session = new CrawlSession(
                jobId,
                startUrl,
                CrawlJobStatus.RUNNING,
                maxPages
        );

        crawlSessionRepository.save(session);
    }

    @Transactional
    public void updateProgress(
            String jobId,
            int visitedPages,
            int collectedContacts
    ) {

        CrawlSession session = findEntity(jobId);

        session.updateProgress(
                visitedPages,
                collectedContacts
        );
    }

    @Transactional
    public void finishSession(
            String jobId,
            CrawlJobStatus status,
            int visitedPages,
            List<ParserResponse> parserResponses
    ) {

        CrawlSession session = findEntity(jobId);

        session.clearContacts();

        if (parserResponses != null) {
            for (ParserResponse response : parserResponses) {

                if (response == null) {
                    continue;
                }

                Contact contact = new Contact(
                        response.organizationName(),
                        response.personName(),
                        response.department(),
                        response.position(),
                        response.email(),
                        response.phone(),
                        response.sourceUrl()
                );

                session.addContact(contact);
            }
        }

        session.updateProgress(
                visitedPages,
                session.getContacts().size()
        );

        session.changeStatus(status);
    }

    @Transactional
    public void updateStatus(
            String jobId,
            CrawlJobStatus status,
            int visitedPages,
            int collectedContacts
    ) {

        CrawlSession session = findEntity(jobId);

        session.updateProgress(
                visitedPages,
                collectedContacts
        );

        session.changeStatus(status);
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(String jobId) {
        return SessionResponse.from(
                findEntity(jobId)
        );
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions() {

        return crawlSessionRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> getContacts(String jobId) {

        if (!crawlSessionRepository.existsByJobId(jobId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "존재하지 않는 수집 세션입니다."
            );
        }

        return contactRepository
                .findByCrawlSessionJobIdOrderByIdAsc(jobId)
                .stream()
                .map(ContactResponse::from)
                .toList();
    }

    private CrawlSession findEntity(String jobId) {

        return crawlSessionRepository
                .findByJobId(jobId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "존재하지 않는 수집 세션입니다."
                        )
                );
    }
}
