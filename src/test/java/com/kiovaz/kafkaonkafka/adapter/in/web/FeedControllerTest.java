package com.kiovaz.kafkaonkafka.adapter.in.web;

import com.kiovaz.kafkaonkafka.application.LiveFeed;
import com.kiovaz.kafkaonkafka.domain.Emotion;
import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedControllerTest {

    private final LiveFeed feed = new LiveFeed();
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FeedController(feed)).build();

    @Test
    void statsOfAnEmptyFeed() throws Exception {
        mvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seq").value(0))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.counts.despair").value(0))
                .andExpect(jsonPath("$.latest").isEmpty());
    }

    @Test
    void statsHaveLowercaseEmotionKeysAndTheLatestResults() throws Exception {
        feed.record(new EmotionResult("q-1", "some text", Emotion.DESPAIR, "Book", "I", Instant.parse("2026-10-03T14:22:10Z")));

        mvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seq").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.counts.despair").value(1))
                .andExpect(jsonPath("$.counts.alienation").value(0))
                .andExpect(jsonPath("$.latest[0].quoteId").value("q-1"))
                .andExpect(jsonPath("$.latest[0].emotion").value("despair"))
                .andExpect(jsonPath("$.latest[0].classifiedAt").value("2026-10-03T14:22:10Z"));
    }

    @Test
    void eventsSendsNewResultsOnceWithTheirNumber() throws Exception {
        MvcResult stream = mvc.perform(get("/events")).andExpect(request().asyncStarted()).andReturn();
        EmotionResult first = new EmotionResult("q-1", "text", Emotion.DESPAIR, "Book", "I", Instant.parse("2026-10-03T14:22:10Z"));

        feed.record(first);
        feed.record(first);   // duplicate: no second event

        String body = stream.getResponse().getContentAsString();
        assertTrue(body.contains("id:1"), body);
        assertTrue(body.contains("\"quoteId\":\"q-1\""), body);
        assertEquals(1, body.split("\"quoteId\":\"q-1\"", -1).length - 1, body);
    }
}
