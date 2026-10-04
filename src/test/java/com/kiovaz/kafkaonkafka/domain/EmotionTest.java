package com.kiovaz.kafkaonkafka.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmotionTest {

    @Test
    void readsTheExactWord() {
        for (Emotion emotion : Emotion.values()) {
            assertEquals(emotion, Emotion.parse(emotion.json()));
        }
    }

    @Test
    void ignoresCaseSpacesAndPunctuation() {
        assertEquals(Emotion.ALIENATION, Emotion.parse("  Alienation.\n"));
        assertEquals(Emotion.DESPAIR, Emotion.parse("\"DESPAIR\"!"));
    }

    @Test
    void onlyTheFirstWordCounts() {
        assertEquals(Emotion.DESPAIR, Emotion.parse("despair and fear"));
        assertEquals(Emotion.UNKNOWN, Emotion.parse("The tone is despair"));
    }

    @Test
    void anythingElseIsUnknown() {
        assertEquals(Emotion.UNKNOWN, Emotion.parse("joy"));
        assertEquals(Emotion.UNKNOWN, Emotion.parse(null));
        assertEquals(Emotion.UNKNOWN, Emotion.parse("   "));
        assertEquals(Emotion.UNKNOWN, Emotion.parse("..."));
    }
}
