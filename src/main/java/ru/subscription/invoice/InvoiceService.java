package ru.subscription.invoice;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.subscription.outbox.InvoiceCreatedEvent;
import ru.subscription.outbox.OutboxService;
import ru.subscription.outbox.OutboxEventType;
import ru.subscription.subscription.Subscription;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceRepository repository;
    private final OutboxService outbox;

    @Transactional
    public Optional<Invoice> createIfDue(Subscription subscription, LocalDate today) {
        LocalDate billingDate = billingDate(subscription.getActivationDate(), today);
        log.debug("Checking invoice for subscription {} and billing date {}", subscription.getId(), billingDate);
        return createForDate(subscription, billingDate);
    }

    @Transactional
    public void createAllDue(Subscription subscription, LocalDate today) {
        if (today.isBefore(subscription.getActivationDate())) {
            return;
        }
        log.debug("Creating due invoices for subscription {} as of {}", subscription.getId(), today);
        long months = monthsBetween(subscription.getActivationDate(), today);
        for (long month = 0; month <= months; month++) {
            LocalDate billingDate = anniversary(subscription.getActivationDate(), month);
            if (!billingDate.isAfter(today)) {
                createForDate(subscription, billingDate);
            }
        }
    }

    private Optional<Invoice> createForDate(Subscription subscription, LocalDate billingDate) {
        if (billingDate == null) {
            log.debug("Invoice is not due for subscription {}", subscription.getId());
            return Optional.empty();
        }
        int inserted = repository.insertIfAbsent(
                subscription.getId(),
                subscription.getUserId(),
                billingDate,
                subscription.getActivationDate(),
                subscription.getType().title(),
                subscription.getType().priceRubles()
        );
        if (inserted == 0) {
            log.debug(
                    "Invoice already exists for subscription {} and billing date {}",
                    subscription.getId(),
                    billingDate
            );
            return Optional.empty();
        }

        Invoice invoice = repository
                .findBySubscriptionIdAndBillingDate(subscription.getId(), billingDate)
                .orElseThrow();
        InvoiceCreatedEvent event = new InvoiceCreatedEvent(
                UUID.randomUUID(),
                invoice.getUserId(),
                subscription.getId(),
                invoice.getId(),
                invoice.getBillingDate(),
                invoice.getActivationDate(),
                invoice.getSubscriptionTitle(),
                invoice.getPriceRubles()
        );
        outbox.add(OutboxEventType.INVOICE_CREATED, event);
        log.info("Created invoice {} for subscription {}", invoice.getId(), subscription.getId());
        return Optional.of(invoice);
    }

    public static LocalDate billingDate(LocalDate activationDate, LocalDate today) {
        if (today.isBefore(activationDate)) {
            return null;
        }
        LocalDate candidate = anniversary(activationDate, monthsBetween(activationDate, today));
        if (candidate.isAfter(today)) {
            candidate = anniversary(
                    activationDate,
                    monthsBetween(activationDate, today) - 1
            );
        }
        return candidate;
    }

    private static long monthsBetween(LocalDate start, LocalDate end) {
        return java.time.temporal.ChronoUnit.MONTHS.between(
                YearMonth.from(start),
                YearMonth.from(end)
        );
    }

    private static LocalDate anniversary(LocalDate source, long months) {
        YearMonth target = YearMonth.from(source).plusMonths(months);
        return target.atDay(Math.min(source.getDayOfMonth(), target.lengthOfMonth()));
    }
}
