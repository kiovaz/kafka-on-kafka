package com.kiovaz.kafkaonkafka.domain;

import java.time.Instant;

public record EmotionResult(String quoteId, String text, Emotion emotion, String book, String part,
                            Instant classifiedAt) {
}
