package ru.subscription.subscription;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.subscription.invoice.InvoiceService;
import ru.subscription.outbox.OutboxEventType;
import ru.subscription.outbox.OutboxService;
import ru.subscription.outbox.SubscriptionActivatedEvent;
import ru.subscription.outbox.SubscriptionDeactivatedEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {
    private static final ZoneOffset BUSINESS_ZONE = ZoneOffset.UTC;
    private final SubscriptionRepository repository;
    private final InvoiceService invoices;
    private final OutboxService outbox;

    @Transactional
    public Subscription activate(Long userId, SubscriptionType type, LocalDate activationDate) {
        log.debug("Activating {} subscription for user {} on {}", type, userId, activationDate);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (activationDate.isBefore(today)) {
            throw new IllegalArgumentException("Activation date cannot be in the past");
        }
        if (repository.findByUserIdAndDeactivationDateIsNull(userId).isPresent()) {
            throw new SubscriptionAlreadyActiveException();
        }
        try {
            Subscription subscription = repository.saveAndFlush(
                    new Subscription(userId, type, activationDate)
            );
            long sequence = repository.nextSubscriptionEventSequence();
            SubscriptionActivatedEvent event = new SubscriptionActivatedEvent(
                    UUID.randomUUID(),
                    sequence,
                    userId,
                    subscription.getId(),
                    type,
                    activationDate
            );
            outbox.add(
                    OutboxEventType.SUBSCRIPTION_ACTIVATED,
                    event,
                    activationDate.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime()
            );
            if (activationDate.isEqual(today)) {
                invoices.createIfDue(subscription, activationDate);
            }
            log.info("Activated {} subscription for user {}", type, userId);
            return subscription;
        } catch (DataIntegrityViolationException exception) {
            throw new SubscriptionAlreadyActiveException();
        }
    }

    @Transactional
    public void deactivate(Long userId, SubscriptionType type) {
        log.debug("Deactivating {} subscription for user {}", type, userId);
        Subscription subscription = repository.findByUserIdAndDeactivationDateIsNull(userId)
                .filter(activeSubscription -> activeSubscription.getType() == type)
                .orElseThrow(SubscriptionNotFoundException::new);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate deactivationDate = today.isAfter(subscription.getActivationDate())
                ? today
                : subscription.getActivationDate();
        subscription.deactivate(deactivationDate);
        long sequence = repository.nextSubscriptionEventSequence();
        SubscriptionDeactivatedEvent event = new SubscriptionDeactivatedEvent(
                UUID.randomUUID(),
                sequence,
                userId,
                subscription.getId(),
                type,
                subscription.getActivationDate()
        );
        if (subscription.getActivationDate().isAfter(today)) {
            outbox.add(
                    OutboxEventType.SUBSCRIPTION_DEACTIVATED,
                    event,
                    subscription.getActivationDate()
                            .atStartOfDay(BUSINESS_ZONE)
                            .toOffsetDateTime()
            );
        } else {
            outbox.add(OutboxEventType.SUBSCRIPTION_DEACTIVATED, event);
        }
        log.info("Deactivated {} subscription for user {}", type, userId);
    }
}
