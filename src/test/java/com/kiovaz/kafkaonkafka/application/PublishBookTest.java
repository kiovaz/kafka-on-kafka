package com.kiovaz.kafkaonkafka.application;

import com.kiovaz.kafkaonkafka.adapter.out.book.BookReader;
import com.kiovaz.kafkaonkafka.adapter.out.kafka.QuotePublisher;
import com.kiovaz.kafkaonkafka.domain.Quote;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class PublishBookTest {

    private final Quote q1 = new Quote("q-0001", "one", "B", "I");
    private final Quote q2 = new Quote("q-0002", "two", "B", "I");
    private final Quote q3 = new Quote("q-0003", "three", "B", "II");
    private final QuotePublisher publisher = mock(QuotePublisher.class);

    private PublishBook book() {
        BookReader reader = mock(BookReader.class);
        when(reader.read()).thenReturn(List.of(q1, q2, q3));
        return new PublishBook(reader, publisher);
    }

    @Test
    void publishesInOrderAndStopsAtTheEnd() {
        PublishBook book = book();
        for (int i = 0; i < 6; i++) {
            book.publishNext();
        }

        InOrder order = inOrder(publisher);
        order.verify(publisher).publish(q1);
        order.verify(publisher).publish(q2);
        order.verify(publisher).publish(q3);
        verifyNoMoreInteractions(publisher);
    }

    @Test
    void retriesTheSameQuoteAfterAFailure() {
        doNothing().when(publisher).publish(q1);
        doThrow(new IllegalStateException("kafka down")).doNothing().when(publisher).publish(q2);
        PublishBook book = book();

        book.publishNext();                                            // q1 ok
        assertThrows(IllegalStateException.class, book::publishNext);  // q2 fails
        book.publishNext();                                            // q2 again, ok
        book.publishNext();                                            // q3

        InOrder order = inOrder(publisher);
        order.verify(publisher).publish(q1);
        order.verify(publisher, org.mockito.Mockito.times(2)).publish(q2);
        order.verify(publisher).publish(q3);
        verifyNoMoreInteractions(publisher);
    }
}
