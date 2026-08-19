package ru.subscription.outbox;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxService outboxService;
    private final RabbitTemplate rabbitTemplate;
    private final String publisherId = UUID.randomUUID().toString();

    @Scheduled(fixedDelayString = "${app.outbox.delay-ms:5000}")
    public void publishPending() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<OutboxEvent> events = outboxService.claimReadyEvents(publisherId, now);
        if (!events.isEmpty()) {
            log.debug("Found {} unpublished outbox events", events.size());
        }
        for (OutboxEvent event : events) {
            try {
                rabbitTemplate.invoke(operations -> {
                    operations.convertAndSend(
                            "subscription.events",
                            event.getType(),
                            event.getPayload()
                    );
                    operations.waitForConfirmsOrDie(5_000);
                    return null;
                });
                boolean marked = outboxService.markPublished(
                        event.getId(),
                        publisherId,
                        OffsetDateTime.now(ZoneOffset.UTC)
                );
                if (!marked) {
                    log.warn(
                            "Outbox event {} was published, but its lease is no longer owned by publisher {}",
                            event.getId(),
                            publisherId
                    );
                    continue;
                }
                log.debug(
                        "Published outbox event {} of type {}",
                        event.getId(),
                        event.getType()
                );
            } catch (RuntimeException exception) {
                log.warn(
                        "Cannot publish outbox event {}: {}",
                        event.getId(),
                        exception.getMessage()
                );
                break;
            }
        }
    }
}
