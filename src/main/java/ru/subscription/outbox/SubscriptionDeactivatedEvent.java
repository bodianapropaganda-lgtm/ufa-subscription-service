package ru.subscription.outbox;

import java.time.LocalDate;
import java.util.UUID;

import ru.subscription.subscription.SubscriptionType;

public record SubscriptionDeactivatedEvent(
        UUID eventId,
        long sequence,
        Long userId,
        Long subscriptionId,
        SubscriptionType subscriptionType,
        LocalDate activationDate
) {
}
