package ru.subscription.cache;

import java.time.LocalDate;

public record CachedSubscription(
        Long subscriptionId,
        long version,
        String type,
        LocalDate activationDate
) {
}
