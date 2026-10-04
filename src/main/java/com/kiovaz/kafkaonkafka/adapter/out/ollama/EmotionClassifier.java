package com.kiovaz.kafkaonkafka.adapter.out.ollama;

import com.kiovaz.kafkaonkafka.domain.Emotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class EmotionClassifier {

    private static final Logger log = LoggerFactory.getLogger(EmotionClassifier.class);
    private static final int ATTEMPTS = 3;
    private static final String PROMPT = "Classify the emotional tone of this excerpt in exactly one word from this list: "
            + "anxiety, absurdity, resignation, alienation, bureaucracy, despair, confusion. "
            + "Respond with only the word.\n\nExcerpt: \"%s\"";

    private final RestClient ollama;
    private final String model;
    private final long retryDelayMs;

    @Autowired
    EmotionClassifier(@Value("${app.ollama.url}") String url, @Value("${app.ollama.model}") String model,
                      @Value("${app.ollama.timeout-seconds}") int timeoutSeconds,
                      @Value("${app.ollama.retry-delay-ms}") long retryDelayMs) {
        this(RestClient.builder().baseUrl(url).requestFactory(factory(timeoutSeconds)).build(), model, retryDelayMs);
    }

    EmotionClassifier(RestClient ollama, String model, long retryDelayMs) {
        this.ollama = ollama;
        this.model = model;
        this.retryDelayMs = retryDelayMs;
    }

    /**
     * Asks the model, trying up to 3 times. If every attempt failed for a reason that can fix itself (Ollama unreachable,
     * timeout, server error, unreadable answer) it throws, so the caller retries the same quote later. A client error
     * (4xx) will not fix itself, so it gives UNKNOWN; an answer outside the emotion set is also UNKNOWN (see Emotion.parse).
     */
    public Emotion classify(String text) {
        boolean canRetryLater = false;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                return Emotion.parse(ask(text));
            } catch (RuntimeException e) {
                canRetryLater = !(e instanceof HttpClientErrorException);
                log.warn("Ollama call failed (attempt {}/{}): {}", attempt, ATTEMPTS, e.getMessage());
                if (attempt < ATTEMPTS) {
                    pause();
                }
            }
        }
        if (canRetryLater) {
            throw new IllegalStateException("Ollama did not answer after " + ATTEMPTS + " attempts");
        }
        log.error("Giving up after {} failed attempts, recording the quote as unknown", ATTEMPTS);
        return Emotion.UNKNOWN;
    }

    private String ask(String text) {
        Answer answer = ollama.post().uri("/api/generate")
                .body(Map.of("model", model, "prompt", PROMPT.formatted(text), "stream", false,
                        "options", Map.of("temperature", 0)))   // 0: the same excerpt always gets the same answer
                .retrieve().body(Answer.class);
        if (answer == null || answer.response() == null) {
            throw new IllegalStateException("Ollama answered without a response field");
        }
        return answer.response();
    }

    private void pause() {
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", e);
        }
    }

    private static JdkClientHttpRequestFactory factory(int timeoutSeconds) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return factory;
    }

    private record Answer(String response) {
    }
}
