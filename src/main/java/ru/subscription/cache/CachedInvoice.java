package ru.subscription.cache;

import java.time.LocalDate;

public record CachedInvoice(
        Long id,
        LocalDate billingDate,
        LocalDate activationDate,
        String subscriptionTitle,
        int priceRubles
) {
}
