package com.kiovaz.kafkaonkafka.adapter.out.persistence;

import com.kiovaz.kafkaonkafka.domain.EmotionResult;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class ResultRepository {

    private final JdbcClient jdbc;

    public ResultRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts the result; a result for a (book, quote) already stored is ignored. Returns true when a row was added. */
    public boolean save(EmotionResult result) {
        int rows = jdbc.sql("""
                        INSERT INTO classified_quotes (quote_id, book, part, text, emotion, classified_at)
                        VALUES (:quoteId, :book, :part, :text, :emotion, :classifiedAt)
                        ON CONFLICT DO NOTHING
                        """)
                .param("quoteId", result.quoteId())
                .param("book", result.book())
                .param("part", result.part(), Types.VARCHAR)
                .param("text", result.text())
                .param("emotion", result.emotion().json())
                .param("classifiedAt", OffsetDateTime.ofInstant(result.classifiedAt(), ZoneOffset.UTC))
                .update();
        return rows > 0;
    }
}
