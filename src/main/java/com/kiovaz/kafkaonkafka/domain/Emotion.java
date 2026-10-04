package com.kiovaz.kafkaonkafka.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Emotion {
    ANXIETY, ABSURDITY, RESIGNATION, ALIENATION, BUREAUCRACY, DESPAIR, CONFUSION, UNKNOWN;

    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Reads the model's raw answer: only its first word counts, anything outside the set is UNKNOWN. */
    public static Emotion parse(String answer) {
        if (answer == null) {
            return UNKNOWN;
        }
        String cleaned = answer.toLowerCase(Locale.ROOT).replaceFirst("^[^\\p{L}]+", "");
        String word = cleaned.split("[^\\p{L}]+")[0];
        for (Emotion emotion : values()) {
            if (emotion.json().equals(word)) {
                return emotion;
            }
        }
        return UNKNOWN;
    }
}
