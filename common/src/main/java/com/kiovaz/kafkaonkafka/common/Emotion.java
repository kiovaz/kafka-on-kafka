package com.kiovaz.kafkaonkafka.common;

/** Lowercase constants so Jackson's default mapping gives the JSON/DB text values. */
public enum Emotion {
    anxiety, absurdity, resignation, alienation, bureaucracy, despair, confusion,
    /** Fallback, never sent in the prompt. */
    unknown
}
