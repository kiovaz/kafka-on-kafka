package com.kiovaz.kafkaonkafka.adapter.out.ollama;

import com.kiovaz.kafkaonkafka.domain.Emotion;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmotionClassifierTest {

    private static final String URL = "http://ollama/api/generate";

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://ollama");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final EmotionClassifier classifier = new EmotionClassifier(builder.build(), "test-model", 0);

    @Test
    void sendsTheModelAndThePrompt() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.options.temperature").value(0))
                .andExpect(jsonPath("$.prompt").value(containsString("the excerpt text")))
                .andExpect(jsonPath("$.prompt").value(containsString("anxiety, absurdity, resignation, alienation, bureaucracy, despair, confusion")))
                .andRespond(answer("alienation"));

        classifier.classify("the excerpt text");

        server.verify();
    }

    @Test
    void readsTheAnswerAndIgnoresTheOtherFields() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"model\":\"test-model\",\"response\":\"Alienation.\",\"done\":true,\"total_duration\":123}",
                MediaType.APPLICATION_JSON));

        assertEquals(Emotion.ALIENATION, classifier.classify("text"));
    }

    @Test
    void recoversOnTheThirdAttempt() {
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(answer("despair"));

        assertEquals(Emotion.DESPAIR, classifier.classify("text"));
        server.verify();
    }

    @Test
    void aServerErrorThreeTimesThrowsSoTheQuoteIsRetriedLater() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(URL)).andRespond(withServerError());
        }

        assertThrows(IllegalStateException.class, () -> classifier.classify("text"));
        server.verify();   // exactly three calls: a fourth would fail the mock server
    }

    @Test
    void aClientErrorThreeTimesGivesUnknownBecauseRetryingCannotHelp() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(URL)).andRespond(withBadRequest());
        }

        assertEquals(Emotion.UNKNOWN, classifier.classify("text"));
        server.verify();
    }

    @Test
    void anAnswerOutsideTheSetGivesUnknownWithoutRetrying() {
        server.expect(requestTo(URL)).andRespond(answer("joy"));

        assertEquals(Emotion.UNKNOWN, classifier.classify("text"));
        server.verify();   // one call only: the model did answer
    }

    @Test
    void whenOllamaCannotBeReachedItThrows() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(URL)).andRespond(withException(new IOException("connection refused")));
        }

        assertThrows(IllegalStateException.class, () -> classifier.classify("text"));
        server.verify();   // it still tried exactly three times
    }

    @Test
    void recoversWhenOllamaComesBackDuringTheAttempts() {
        server.expect(requestTo(URL)).andRespond(withException(new IOException("connection refused")));
        server.expect(requestTo(URL)).andRespond(withException(new IOException("connection refused")));
        server.expect(requestTo(URL)).andRespond(answer("anxiety"));

        assertEquals(Emotion.ANXIETY, classifier.classify("text"));
        server.verify();
    }

    @Test
    void anAnswerWithoutTheResponseFieldCountsAsAFailureToRetryLater() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo(URL)).andRespond(withSuccess("{\"done\":true}", MediaType.APPLICATION_JSON));
        }

        assertThrows(IllegalStateException.class, () -> classifier.classify("text"));
        server.verify();
    }

    private static org.springframework.test.web.client.ResponseCreator answer(String word) {
        return withSuccess("{\"response\":\"" + word + "\",\"done\":true}", MediaType.APPLICATION_JSON);
    }
}
