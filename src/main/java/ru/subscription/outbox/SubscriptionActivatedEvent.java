package ru.subscription.outbox;

import java.time.LocalDate;
import java.util.UUID;

import ru.subscription.subscription.SubscriptionType;

public record SubscriptionActivatedEvent(
        UUID eventId,
        long sequence,
        Long userId,
        Long subscriptionId,
        SubscriptionType subscriptionType,
        LocalDate activationDate
) {
}
