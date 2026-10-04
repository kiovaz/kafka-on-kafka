package com.kiovaz.kafkaonkafka.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ModelsJsonTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void quoteRoundTrip() throws Exception {
        var q = new Quote("q-0042", "text", "The Metamorphosis", "I");
        String json = mapper.writeValueAsString(q);
        assertTrue(json.contains("\"id\"") && json.contains("\"text\"")
                && json.contains("\"book\"") && json.contains("\"part\""));
        assertEquals(q, mapper.readValue(json, Quote.class));
    }

    @Test
    void quoteWithoutPart() throws Exception {
        var q = new Quote("q-0001", "text", "Book", null);
        var back = mapper.readValue(mapper.writeValueAsString(q), Quote.class);
        assertEquals(q, back);
        assertNull(back.part());
    }

    @Test
    void emotionIsLowercaseInJson() throws Exception {
        assertEquals("\"alienation\"", mapper.writeValueAsString(Emotion.alienation));
        assertEquals(Emotion.alienation, mapper.readValue("\"alienation\"", Emotion.class));
        assertEquals(8, Emotion.values().length);
    }

    @Test
    void emotionResultRoundTrip() throws Exception {
        var r = new EmotionResult("q-0042", "text", Emotion.alienation, "The Metamorphosis", "I",
                Instant.parse("2026-10-03T14:22:10Z"));
        String json = mapper.writeValueAsString(r);
        assertTrue(json.contains("\"classifiedAt\":\"2026-10-03T14:22:10Z\""), json);
        assertEquals(r, mapper.readValue(json, EmotionResult.class));
    }
}
