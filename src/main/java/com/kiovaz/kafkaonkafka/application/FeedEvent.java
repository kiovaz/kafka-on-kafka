package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.domain.EmotionResult;

/** A result added to the live view, numbered in the order it was added. */
public record FeedEvent(long id, EmotionResult result) {
}
