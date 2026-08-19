package ru.subscription.invoice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.subscription.subscription.Subscription;
import ru.subscription.subscription.SubscriptionRepository;

class InvoiceSchedulerTest {
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final InvoiceService invoices = mock(InvoiceService.class);
    private final InvoiceScheduler scheduler = new InvoiceScheduler(subscriptions, invoices);

    @Test
    void continuesWhenOneSubscriptionFails() {
        Subscription failed = mock(Subscription.class);
        Subscription next = mock(Subscription.class);
        when(failed.getId()).thenReturn(1L);
        when(subscriptions.findByDeactivationDateIsNullAndActivationDateLessThanEqual(
                any(LocalDate.class)
        ))
                .thenReturn(List.of(failed, next));
        doThrow(new IllegalStateException("database error"))
                .when(invoices)
                .createAllDue(eq(failed), any(LocalDate.class));

        scheduler.issueInvoices();

        verify(invoices).createAllDue(next, LocalDate.now(java.time.ZoneOffset.UTC));
    }
}
