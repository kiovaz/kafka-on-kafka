package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveFeedTest {

    private static final Instant T0 = Instant.parse("2026-10-03T14:00:00Z");

    private final LiveFeed feed = new LiveFeed();

    @Test
    void emptyState() {
        Stats stats = feed.stats();

        assertEquals(0, stats.seq());
        assertEquals(0, stats.total());
        assertEquals(8, stats.counts().size());
        assertTrue(stats.counts().values().stream().allMatch(count -> count == 0L));
        assertTrue(stats.latest().isEmpty());
    }

    @Test
    void countsHaveEveryEmotionWithZerosForTheMissingOnes() {
        feed.record(result("Book", "q-1", Emotion.DESPAIR, 1));
        feed.record(result("Book", "q-2", Emotion.DESPAIR, 2));
        feed.record(result("Book", "q-3", Emotion.ALIENATION, 3));

        Stats stats = feed.stats();

        assertEquals(3, stats.total());
        assertEquals(2L, stats.counts().get("despair"));
        assertEquals(1L, stats.counts().get("alienation"));
        assertEquals(0L, stats.counts().get("bureaucracy"));
        assertEquals(8, stats.counts().size());
    }

    @Test
    void theSameQuoteTwiceCountsOnceAndKeepsTheFirstResult() {
        feed.record(result("Book", "q-1", Emotion.DESPAIR, 1));
        feed.record(result("Book", "q-1", Emotion.ALIENATION, 2));

        Stats stats = feed.stats();

        assertEquals(1, stats.total());
        assertEquals(1L, stats.counts().get("despair"));
        assertEquals(0L, stats.counts().get("alienation"));
        assertEquals(1, stats.seq());
    }

    @Test
    void theSameQuoteIdInAnotherBookIsAnotherQuote() {
        feed.record(result("Book A", "q-1", Emotion.ANXIETY, 1));
        feed.record(result("Book B", "q-1", Emotion.ANXIETY, 2));

        assertEquals(2, feed.stats().total());
    }

    @Test
    void latestHasAtMostTwentyNewestFirst() {
        for (int i = 1; i <= 25; i++) {
            feed.record(result("Book", String.format("q-%02d", i), Emotion.CONFUSION, i));
        }

        List<EmotionResult> latest = feed.stats().latest();

        assertEquals(20, latest.size());
        assertEquals("q-25", latest.getFirst().quoteId());
        assertEquals("q-06", latest.getLast().quoteId());
    }

    @Test
    void subscribersGetNumberedEventsOnlyForNewResults() {
        List<FeedEvent> received = new ArrayList<>();
        feed.subscribe(received::add);

        feed.record(result("Book", "q-1", Emotion.DESPAIR, 1));
        feed.record(result("Book", "q-1", Emotion.DESPAIR, 2));   // duplicate: no event
        feed.record(result("Book", "q-2", Emotion.DESPAIR, 3));

        assertEquals(List.of(1L, 2L), received.stream().map(FeedEvent::id).toList());
        assertEquals(2, feed.stats().seq());
    }

    @Test
    void anUnsubscribedSubscriberGetsNothing() {
        List<FeedEvent> received = new ArrayList<>();
        Runnable unsubscribe = feed.subscribe(received::add);

        unsubscribe.run();
        feed.record(result("Book", "q-1", Emotion.DESPAIR, 1));

        assertTrue(received.isEmpty());
    }

    @Test
    void aFailingSubscriberDoesNotStopTheOthers() {
        List<FeedEvent> received = new ArrayList<>();
        feed.subscribe(event -> {
            throw new IllegalStateException("client gone");
        });
        feed.subscribe(received::add);

        feed.record(result("Book", "q-1", Emotion.DESPAIR, 1));

        assertEquals(1, received.size());
    }

    private static EmotionResult result(String book, String quoteId, Emotion emotion, int secondsAfterStart) {
        return new EmotionResult(quoteId, "text", emotion, book, "I", T0.plusSeconds(secondsAfterStart));
    }
}
