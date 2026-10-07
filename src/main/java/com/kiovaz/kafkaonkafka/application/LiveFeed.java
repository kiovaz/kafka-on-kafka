package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The live view in memory: one result per quote, rebuilt by reading the topic from the start. Everything is computed
 * from the map, so reading the same messages again (a replay, a republished book) changes nothing. Adding and reading
 * are synchronized so that a snapshot and the event numbers always agree.
 */
@Service
public class LiveFeed {

    private static final Logger log = LoggerFactory.getLogger(LiveFeed.class);
    private static final int LATEST = 20;

    private final Map<String, EmotionResult> byQuote = new HashMap<>();
    private final List<Consumer<FeedEvent>> subscribers = new CopyOnWriteArrayList<>();
    private long seq = 0;

    /** Adds the result unless the quote is already known (the first result wins) and tells the subscribers if it was new. */
    public synchronized void record(EmotionResult result) {
        if (byQuote.putIfAbsent(result.book() + "/" + result.quoteId(), result) != null) {
            return;
        }
        FeedEvent event = new FeedEvent(++seq, result);
        for (Consumer<FeedEvent> subscriber : subscribers) {
            try {
                subscriber.accept(event);
            } catch (RuntimeException e) {
                log.warn("A subscriber failed: {}", e.getMessage());   // one bad client must not stop the others
            }
        }
    }

    /** Registers a subscriber for new results; the returned action removes it. */
    public Runnable subscribe(Consumer<FeedEvent> subscriber) {
        subscribers.add(subscriber);
        return () -> subscribers.remove(subscriber);
    }

    public synchronized Stats stats() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Emotion emotion : Emotion.values()) {
            counts.put(emotion.json(), 0L);
        }
        byQuote.values().forEach(result -> counts.merge(result.emotion().json(), 1L, Long::sum));
        List<EmotionResult> latest = byQuote.values().stream()
                .sorted(Comparator.comparing(EmotionResult::classifiedAt).thenComparing(EmotionResult::quoteId).reversed())
                .limit(LATEST)
                .toList();
        return new Stats(seq, byQuote.size(), counts, latest);
    }
}
