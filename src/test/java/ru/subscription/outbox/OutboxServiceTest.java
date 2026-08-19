package ru.subscription.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OutboxServiceTest {
    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final OutboxService service = new OutboxService(repository, mock(ObjectMapper.class));

    @Test
    void claimsEventsForTwoMinutes() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-18T10:00:00Z");
        OutboxEvent event = mock(OutboxEvent.class);
        when(repository.lockReadyEvents(now)).thenReturn(List.of(event));

        service.claimReadyEvents("publisher-1", now);

        verify(event).claim("publisher-1", now.plusMinutes(2));
    }

    @Test
    void reportsLostLeaseWhenEventWasNotMarkedPublished() {
        UUID eventId = UUID.randomUUID();
        OffsetDateTime publishedAt = OffsetDateTime.parse("2026-08-18T10:01:00Z");
        when(repository.markPublished(eventId, "publisher-1", publishedAt)).thenReturn(0);

        boolean marked = service.markPublished(eventId, "publisher-1", publishedAt);

        assertThat(marked).isFalse();
    }
}
