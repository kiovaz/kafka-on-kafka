package com.kiovaz.kafkaonkafka.adapter.in.kafka;

import com.kiovaz.kafkaonkafka.application.LiveFeed;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class FeedListener implements ConsumerSeekAware {

    private final LiveFeed feed;

    FeedListener(LiveFeed feed) {
        this.feed = feed;
    }

    @KafkaListener(topics = "emotions", groupId = "frontend",
            properties = "spring.json.value.default.type=com.kiovaz.kafkaonkafka.domain.EmotionResult")
    void on(EmotionResult result) {
        feed.record(result);
    }

    /** Whenever partitions are given to this group, read them again from the first message (never trust a saved offset). */
    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        callback.seekToBeginning(assignments.keySet());
    }
}
