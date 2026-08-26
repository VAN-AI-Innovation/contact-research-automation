package com.vanai.backend.crawler.sse;

import com.vanai.backend.crawler.CrawlJob;
import com.vanai.backend.crawler.CrawlJobStatus;
import com.vanai.backend.crawler.sse.dto.CrawlProgressEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CrawlEventService {

    private static final long SSE_TIMEOUT =
            30L * 60L * 1000L;

    private final ConcurrentHashMap<
            String,
            CopyOnWriteArrayList<SseEmitter>
            > emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(
            String jobId,
            CrawlJob currentJob
    ) {

        SseEmitter emitter =
                new SseEmitter(SSE_TIMEOUT);

        emitters
                .computeIfAbsent(
                        jobId,
                        key -> new CopyOnWriteArrayList<>()
                )
                .add(emitter);

        emitter.onCompletion(
                () -> removeEmitter(jobId, emitter)
        );

        emitter.onTimeout(() -> {
            removeEmitter(jobId, emitter);
            emitter.complete();
        });

        emitter.onError(error ->
                removeEmitter(jobId, emitter)
        );

        try {

            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(
                                    CrawlProgressEvent.from(
                                            currentJob
                                    )
                            )
            );

        } catch (IOException e) {

            removeEmitter(jobId, emitter);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void sendProgress(CrawlJob job) {

        send(
                job,
                "progress",
                false
        );
    }

    public void sendCompleted(CrawlJob job) {

        send(
                job,
                "completed",
                true
        );
    }

    public void sendStopped(CrawlJob job) {

        send(
                job,
                "stopped",
                true
        );
    }

    public void sendFailed(CrawlJob job) {

        send(
                job,
                "failed",
                true
        );
    }

    private void send(
            CrawlJob job,
            String eventName,
            boolean completeAfterSend
    ) {

        List<SseEmitter> jobEmitters =
                emitters.get(job.getJobId());

        if (jobEmitters == null
                || jobEmitters.isEmpty()) {
            return;
        }

        CrawlProgressEvent event =
                CrawlProgressEvent.from(job);

        for (SseEmitter emitter : jobEmitters) {

            try {

                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(event)
                );

                if (completeAfterSend) {
                    emitter.complete();
                }

            } catch (Exception e) {

                emitter.completeWithError(e);

            } finally {

                if (completeAfterSend) {
                    removeEmitter(
                            job.getJobId(),
                            emitter
                    );
                }
            }
        }
    }

    private void removeEmitter(
            String jobId,
            SseEmitter emitter
    ) {

        CopyOnWriteArrayList<SseEmitter> list =
                emitters.get(jobId);

        if (list == null) {
            return;
        }

        list.remove(emitter);

        if (list.isEmpty()) {
            emitters.remove(jobId);
        }
    }

    public int getSubscriberCount(String jobId) {

        List<SseEmitter> list =
                emitters.get(jobId);

        return list == null
                ? 0
                : list.size();
    }
}
