package ru.subscription.invoice;

import java.time.LocalDate;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.subscription.subscription.SubscriptionRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class InvoiceScheduler {
    private static final ZoneOffset BUSINESS_ZONE = ZoneOffset.UTC;
    private final SubscriptionRepository subscriptions;
    private final InvoiceService invoices;

    @Scheduled(cron = "${app.invoices.cron:0 0 2 * * *}", zone = "UTC")
    public void issueInvoices() {
        try {
            LocalDate today = LocalDate.now(BUSINESS_ZONE);
            log.info("Starting daily invoice job for {}", today);
            subscriptions.findByDeactivationDateIsNullAndActivationDateLessThanEqual(today).forEach(subscription -> {
                try {
                    invoices.createAllDue(subscription, today);
                } catch (RuntimeException e) {
                    log.error("Invoice job failed for subscription {}", subscription.getId(), e);
                }
            });
        } catch (RuntimeException e) {
            log.error("Invoice scheduler failed", e);
        }
    }
}
