package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.domain.EmotionResult;

import java.util.List;
import java.util.Map;

/**
 * What the live view shows: how many quotes are known, how many per emotion (all eight, zeros included) and the latest.
 * {@code seq} is the number of the last event included, so a client can ignore the events it already has.
 */
public record Stats(long seq, int total, Map<String, Long> counts, List<EmotionResult> latest) {
}
