package com.kiovaz.kafkaonkafka.adapter.in.web;

import com.kiovaz.kafkaonkafka.application.LiveFeed;
import com.kiovaz.kafkaonkafka.application.Stats;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
class FeedController {

    private final LiveFeed feed;

    FeedController(LiveFeed feed) {
        this.feed = feed;
    }

    @GetMapping("/stats")
    Stats stats() {
        return feed.stats();
    }

    /** Sends each new result as an event whose id is its sequence number; the browser reconnects by itself. */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events() {
        SseEmitter emitter = new SseEmitter(0L);
        Runnable unsubscribe = feed.subscribe(event -> {
            try {
                emitter.send(SseEmitter.event().id(String.valueOf(event.id())).data(event.result()));
            } catch (IOException | RuntimeException e) {
                emitter.completeWithError(e);
            }
        });
        emitter.onCompletion(unsubscribe);
        emitter.onTimeout(unsubscribe);
        emitter.onError(error -> unsubscribe.run());
        try {
            emitter.send(SseEmitter.event().comment("connected"));   // flushes the headers so the browser sees the stream open
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
