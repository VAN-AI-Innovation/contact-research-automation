package com.vanai.backend.crawler;

import com.vanai.backend.crawler.dto.CrawlStartRequest;
import com.vanai.backend.crawler.dto.CrawlStartResponse;
import com.vanai.backend.crawler.dto.CrawlStatusResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/crawl")
public class CrawlerController {

    private final CrawlerService crawlerService;

    public CrawlerController(
            CrawlerService crawlerService
    ) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/start")
    public ResponseEntity<CrawlStartResponse> start(
            @Valid @RequestBody CrawlStartRequest request
    ) {
        return ResponseEntity.ok(
                crawlerService.start(request)
        );
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<CrawlStatusResponse> status(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(
                crawlerService.getStatus(jobId)
        );
    }

    @PostMapping("/{jobId}/stop")
    public ResponseEntity<CrawlStatusResponse> stop(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok(
                crawlerService.stop(jobId)
        );
    }

     @GetMapping(
                value = "/{jobId}/events",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE
        )
        public SseEmitter events(
                @PathVariable String jobId
        ) {
            return crawlerService.subscribe(jobId);
        }   

}