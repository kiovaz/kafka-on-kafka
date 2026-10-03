package com.kiovaz.kafkaonkafka.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Emotion {
    ANXIETY, ABSURDITY, RESIGNATION, ALIENATION, BUREAUCRACY, DESPAIR, CONFUSION, UNKNOWN;

    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }
}
