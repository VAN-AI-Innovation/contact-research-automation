package com.vanai.backend.crawler;

import com.vanai.backend.crawler.dto.CrawlStartRequest;
import com.vanai.backend.crawler.dto.CrawlStartResponse;
import com.vanai.backend.crawler.dto.CrawlStatusResponse;
import com.vanai.backend.parser.ParserService;
import com.vanai.backend.parser.dto.ParserResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.vanai.backend.contact.ContactDeduplicationService;
import com.vanai.backend.persistence.service.SessionService;
import com.vanai.backend.crawler.sse.CrawlEventService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class CrawlerService {

    private static final long REQUEST_DELAY_MS = 1000L;

    private final ParserService parserService;
    private final TaskExecutor taskExecutor;
    private final ContactDeduplicationService contactDeduplicationService;
    private final SessionService sessionService;
    private final CrawlEventService crawlEventService;

    private final ConcurrentMap<String, CrawlJob> jobs =
            new ConcurrentHashMap<>();

    public CrawlerService(
            ParserService parserService,
            TaskExecutor taskExecutor,
            ContactDeduplicationService contactDeduplicationService,
            SessionService sessionService,
            CrawlEventService crawlEventService
    ) {
        this.parserService = parserService;
        this.taskExecutor = taskExecutor;
        this.contactDeduplicationService = 
                contactDeduplicationService;
        this.sessionService = sessionService;
        this.crawlEventService = crawlEventService;
    }

    public CrawlStartResponse start(CrawlStartRequest request) {

        String startUrl = normalizeUrl(request.url());

        validateUrl(startUrl);

        String jobId = UUID.randomUUID().toString();

        CrawlJob job = new CrawlJob(
                jobId,
                startUrl,
                request.resolvedMaxPages()
        );

        job.setStatus(CrawlJobStatus.RUNNING);
        jobs.put(jobId, job);

        sessionService.createSession(
                job.getJobId(),
                job.getStartUrl(),
                job.getMaxPages()
        );

        taskExecutor.execute(() -> runCrawler(job));

        return new CrawlStartResponse(
                job.getJobId(),
                job.getStatus()
        );
    }

    public CrawlStatusResponse getStatus(String jobId) {
        CrawlJob job = getJob(jobId);
        return CrawlStatusResponse.from(job);
    }

    public SseEmitter subscribe(String jobId) {
        CrawlJob job = getJob(jobId);

        return crawlEventService.subscribe(
                jobId,
                job
        );
    }

    public CrawlStatusResponse stop(String jobId) {

        CrawlJob job = getJob(jobId);

        if (job.getStatus() == CrawlJobStatus.COMPLETED ||
                job.getStatus() == CrawlJobStatus.FAILED) {
            return CrawlStatusResponse.from(job);
        }

        job.requestStop();
        job.setStatus(CrawlJobStatus.STOPPED);

        sessionService.updateStatus(
                job.getJobId(),
                CrawlJobStatus.STOPPED,
                job.getVisitedPages(),
                job.getCollectedContacts()
        );

        crawlEventService.sendStopped(job);

        return CrawlStatusResponse.from(job);
    }

    private CrawlJob getJob(String jobId) {

        CrawlJob job = jobs.get(jobId);

        if (job == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "존재하지 않는 수집 작업입니다."
            );
        }

        return job;
    }

    private void runCrawler(CrawlJob job) {

        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        Set<String> queued = new HashSet<>();

        queue.offer(job.getStartUrl());
        queued.add(job.getStartUrl());

        String baseHost = getNormalizedHost(job.getStartUrl());

        try {

            while (!queue.isEmpty()
                    && job.getVisitedPages() < job.getMaxPages()) {

                if (job.isStopRequested()) {

                    job.setStatus(CrawlJobStatus.STOPPED);

                    sessionService.finishSession(
                            job.getJobId(),
                            CrawlJobStatus.STOPPED,
                            job.getVisitedPages(),
                            job.getContacts()
                    );

                    crawlEventService.sendStopped(job);

                    return;
                }

                String currentUrl = queue.poll();

                if (currentUrl == null || visited.contains(currentUrl)) {
                    continue;
                }

                visited.add(currentUrl);
                job.incrementVisitedPages();

                sessionService.updateProgress(
                        job.getJobId(),
                        job.getVisitedPages(),
                        job.getCollectedContacts()
                );

                try {

                    ParserResponse response =
                            parserService.parse(currentUrl);

                    if (hasContact(response)) {

                        var updatedContacts = job.getContacts();

                        updatedContacts.add(response);

                        job.replaceContacts(
                                contactDeduplicationService
                                        .deduplicate(updatedContacts)
                        );

                        sessionService.updateProgress(
                                job.getJobId(),
                                job.getVisitedPages(),
                                job.getCollectedContacts()
                        );
                    }

                    crawlEventService.sendProgress(job);

                } catch (Exception ignored) {
                    // 개별 페이지 파싱 실패는 전체 작업 실패로 처리하지 않음
                }

                if (job.isStopRequested()) {

                    job.setStatus(CrawlJobStatus.STOPPED);

                    sessionService.finishSession(
                            job.getJobId(),
                            CrawlJobStatus.STOPPED,
                            job.getVisitedPages(),
                            job.getContacts()
                    );

                    crawlEventService.sendStopped(job);
                    
                    return;
                }

                sleepRateLimit(job);

                try {

                    Document document = Jsoup.connect(currentUrl)
                            .userAgent(
                                    "Mozilla/5.0 ContactResearchBot/1.0"
                            )
                            .timeout(10000)
                            .followRedirects(true)
                            .get();

                    for (Element link : document.select("a[href]")) {

                        String discoveredUrl =
                                normalizeUrl(link.absUrl("href"));

                        if (discoveredUrl == null ||
                                discoveredUrl.isBlank()) {
                            continue;
                        }

                        if (!isHttpUrl(discoveredUrl)) {
                            continue;
                        }

                        if (!isSameDomain(
                                baseHost,
                                discoveredUrl
                        )) {
                            continue;
                        }

                        if (!isHtmlCandidate(discoveredUrl)) {
                            continue;
                        }

                        if (!visited.contains(discoveredUrl)
                                && queued.add(discoveredUrl)) {
                            queue.offer(discoveredUrl);
                        }
                    }

                } catch (Exception e) {

                    if (currentUrl.equals(job.getStartUrl())) {
                        throw new IllegalStateException(
                                "시작 URL에 접속할 수 없습니다: " + currentUrl,
                                e
                        );
                    }

                    // 중간 페이지 링크 탐색 실패는
                    // 전체 Crawl 실패로 처리하지 않고 다음 URL로 진행
                }

                if (!queue.isEmpty()
                        && job.getVisitedPages() < job.getMaxPages()) {
                    sleepRateLimit(job);
                }
            }

            if (job.isStopRequested()) {

                job.setStatus(CrawlJobStatus.STOPPED);

                sessionService.finishSession(
                        job.getJobId(),
                        CrawlJobStatus.STOPPED,
                        job.getVisitedPages(),
                        job.getContacts()
                );
                
            } else {

                job.setStatus(CrawlJobStatus.COMPLETED);

                sessionService.finishSession(
                        job.getJobId(),
                        CrawlJobStatus.COMPLETED,
                        job.getVisitedPages(),
                        job.getContacts()
                );

                crawlEventService.sendCompleted(job);
            }

        } catch (Exception e) {

            if (job.isStopRequested()) {

                job.setStatus(CrawlJobStatus.STOPPED);

                sessionService.finishSession(
                        job.getJobId(),
                        CrawlJobStatus.STOPPED,
                        job.getVisitedPages(),
                        job.getContacts()
                );

            } else {

                job.setStatus(CrawlJobStatus.FAILED);

                sessionService.finishSession(
                        job.getJobId(),
                        CrawlJobStatus.FAILED,
                        job.getVisitedPages(),
                        job.getContacts()
                );

                crawlEventService.sendFailed(job);
            }
        }
    }

    private void sleepRateLimit(CrawlJob job)
            throws InterruptedException {

        long remaining = REQUEST_DELAY_MS;

        while (remaining > 0) {

            if (job.isStopRequested()) {
                return;
            }

            long sleepTime = Math.min(remaining, 200L);

            Thread.sleep(sleepTime);

            remaining -= sleepTime;
        }
    }

    private void validateUrl(String url) {

        if (!isHttpUrl(url)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "올바른 http/https URL을 입력해주세요."
            );
        }
    }

    private boolean isHttpUrl(String url) {

        if (url == null || url.isBlank()) {
            return false;
        }

        try {

            URI uri = URI.create(url);

            String scheme = uri.getScheme();
            String host = uri.getHost();

            return host != null
                    && scheme != null
                    && (
                    scheme.equalsIgnoreCase("http")
                            || scheme.equalsIgnoreCase("https")
            );

        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeUrl(String url) {

        if (url == null || url.isBlank()) {
            return null;
        }

        try {

            URI uri = URI.create(url.trim());

            URI normalized = new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );

            String result = normalized.toString();

            while (result.endsWith("/")
                    && result.length()
                    > (normalized.getScheme() + "://").length() + 1) {
                result = result.substring(
                        0,
                        result.length() - 1
                );
            }

            return result;

        } catch (Exception e) {
            return url.trim();
        }
    }

    private String getNormalizedHost(String url) {

        try {

            String host = URI.create(url).getHost();

            if (host == null) {
                return "";
            }

            host = host.toLowerCase();

            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host;

        } catch (Exception e) {
            return "";
        }
    }

    private boolean isSameDomain(
            String baseHost,
            String candidateUrl
    ) {

        String candidateHost =
                getNormalizedHost(candidateUrl);

        return !baseHost.isBlank()
                && baseHost.equals(candidateHost);
    }

    private boolean isHtmlCandidate(String url) {

        String lower = url.toLowerCase();

        return !(lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".gif")
                || lower.endsWith(".svg")
                || lower.endsWith(".webp")
                || lower.endsWith(".pdf")
                || lower.endsWith(".zip")
                || lower.endsWith(".hwp")
                || lower.endsWith(".hwpx")
                || lower.endsWith(".doc")
                || lower.endsWith(".docx")
                || lower.endsWith(".xls")
                || lower.endsWith(".xlsx")
                || lower.endsWith(".ppt")
                || lower.endsWith(".pptx")
                || lower.endsWith(".mp3")
                || lower.endsWith(".mp4"));
    }

    private boolean hasContact(ParserResponse response) {

        if (response == null) {
            return false;
        }

        boolean hasEmail =
                response.email() != null
                        && !response.email().isBlank();

        boolean hasPhone =
                response.phone() != null
                        && !response.phone().isBlank();

        return hasEmail || hasPhone;
}

}