package ru.subscription.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxService {
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(2);
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void add(OutboxEventType type, Object payload) {
        add(type, payload, OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void add(OutboxEventType type, Object payload, OffsetDateTime availableAt) {
        try {
            String serializedPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent(
                    type.routingKey(),
                    serializedPayload,
                    OffsetDateTime.now(ZoneOffset.UTC),
                    availableAt
            );
            repository.save(event);
            log.debug("Stored {} event in outbox", type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize outbox event", e);
        }
    }

    @Transactional
    public List<OutboxEvent> claimReadyEvents(String publisherId, OffsetDateTime now) {
        List<OutboxEvent> events = repository.lockReadyEvents(now);
        OffsetDateTime leaseUntil = now.plus(CLAIM_LEASE);
        events.forEach(event -> event.claim(publisherId, leaseUntil));
        return events;
    }

    @Transactional
    public boolean markPublished(
            UUID eventId,
            String publisherId,
            OffsetDateTime publishedAt
    ) {
        return repository.markPublished(eventId, publisherId, publishedAt) == 1;
    }
}
