package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.adapter.out.kafka.EmotionResultPublisher;
import com.kiovaz.kafkaonkafka.adapter.out.ollama.EmotionClassifier;
import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import com.kiovaz.kafkaonkafka.domain.Quote;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClassifyQuoteTest {

    private final EmotionClassifier classifier = mock(EmotionClassifier.class);
    private final EmotionResultPublisher publisher = mock(EmotionResultPublisher.class);

    @Test
    void publishesTheResultBuiltFromTheQuoteAndTheEmotion() {
        Quote quote = new Quote("q-0042", "some text", "The Metamorphosis", "I");
        when(classifier.classify("some text")).thenReturn(Emotion.DESPAIR);
        Instant before = Instant.now();

        new ClassifyQuote(classifier, publisher).handle(quote);

        ArgumentCaptor<EmotionResult> sent = ArgumentCaptor.forClass(EmotionResult.class);
        verify(publisher, times(1)).publish(sent.capture());
        EmotionResult result = sent.getValue();
        assertEquals("q-0042", result.quoteId());
        assertEquals("some text", result.text());
        assertEquals(Emotion.DESPAIR, result.emotion());
        assertEquals("The Metamorphosis", result.book());
        assertEquals("I", result.part());
        assertFalse(result.classifiedAt().isBefore(before));
    }

    @Test
    void publishesNothingWhenTheClassifierCannotAnswer() {
        when(classifier.classify("text")).thenThrow(new IllegalStateException("Ollama is unreachable"));

        assertThrows(IllegalStateException.class,
                () -> new ClassifyQuote(classifier, publisher).handle(new Quote("q-1", "text", "B", null)));

        verifyNoInteractions(publisher);
    }
}
