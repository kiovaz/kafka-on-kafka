package com.kiovaz.kafkaonkafka.adapter.in.kafka;

import com.kiovaz.kafkaonkafka.application.LiveFeed;
import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.listener.ConsumerSeekAware;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FeedListenerTest {

    @Test
    void goesBackToTheBeginningWhenPartitionsAreAssigned() {
        ConsumerSeekAware.ConsumerSeekCallback callback = mock(ConsumerSeekAware.ConsumerSeekCallback.class);
        TopicPartition p0 = new TopicPartition("emotions", 0);
        TopicPartition p2 = new TopicPartition("emotions", 2);

        new FeedListener(new LiveFeed()).onPartitionsAssigned(Map.of(p0, 5L, p2, 9L), callback);

        ArgumentCaptor<Collection<TopicPartition>> seek = ArgumentCaptor.forClass(Collection.class);
        verify(callback).seekToBeginning(seek.capture());
        assertEquals(Set.of(p0, p2), new HashSet<>(seek.getValue()));
    }

    @Test
    void givesEachResultToTheFeed() {
        LiveFeed feed = new LiveFeed();

        new FeedListener(feed).on(
                new EmotionResult("q-1", "text", Emotion.DESPAIR, "Book", "I", Instant.parse("2026-10-03T14:00:00Z")));

        assertEquals(1, feed.stats().total());
    }
}
