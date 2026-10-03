package com.kiovaz.kafkaonkafka.domain;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void emotionIsLowercaseInJson() {
        assertEquals("\"alienation\"", json.writeValueAsString(Emotion.ALIENATION));
        assertEquals(Emotion.UNKNOWN, json.readValue("\"unknown\"", Emotion.class));
    }

    @Test
    void emotionOutsideTheSetIsRejected() {
        assertThrows(JacksonException.class, () -> json.readValue("\"joy\"", Emotion.class));
    }

    @Test
    void emotionResultJsonMatchesTheReadme() {
        var result = new EmotionResult("q-0042", "text", Emotion.ALIENATION, "The Metamorphosis", "I",
                Instant.parse("2026-10-03T14:22:10Z"));
        String out = json.writeValueAsString(result);
        assertTrue(out.contains("\"emotion\":\"alienation\""), out);
        assertTrue(out.contains("\"classifiedAt\":\"2026-10-03T14:22:10Z\""), out);
        assertEquals(result, json.readValue(out, EmotionResult.class));
    }

    @Test
    void nullPartRoundTrips() {
        var quote = new Quote("q-0001", "text", "The Metamorphosis", null);
        assertEquals(quote, json.readValue(json.writeValueAsString(quote), Quote.class));
        var result = new EmotionResult("q-0001", "text", Emotion.UNKNOWN, "The Metamorphosis", null, Instant.EPOCH);
        assertEquals(result, json.readValue(json.writeValueAsString(result), EmotionResult.class));
    }
}
