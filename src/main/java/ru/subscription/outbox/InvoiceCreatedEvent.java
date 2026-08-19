package ru.subscription.outbox;

import java.time.LocalDate;
import java.util.UUID;

public record InvoiceCreatedEvent(
        UUID eventId,
        Long userId,
        Long subscriptionId,
        Long invoiceId,
        LocalDate billingDate,
        LocalDate activationDate,
        String subscriptionTitle,
        int priceRubles
) {
}
