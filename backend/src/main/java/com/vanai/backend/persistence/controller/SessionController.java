package com.vanai.backend.persistence.controller;

import com.vanai.backend.persistence.dto.ContactResponse;
import com.vanai.backend.persistence.dto.SessionResponse;
import com.vanai.backend.persistence.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<List<SessionResponse>> getSessions() {
        return ResponseEntity.ok(
                sessionService.getSessions()
        );
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(
                sessionService.getSession(jobId)
        );
    }

    @GetMapping("/{jobId}/contacts")
    public ResponseEntity<List<ContactResponse>> getContacts(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(
                sessionService.getContacts(jobId)
        );
    }
}
