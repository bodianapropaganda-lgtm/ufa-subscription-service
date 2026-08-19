package ru.subscription.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.subscription.outbox.InvoiceCreatedEvent;
import ru.subscription.outbox.OutboxEventType;
import ru.subscription.outbox.OutboxService;
import ru.subscription.subscription.Subscription;
import ru.subscription.subscription.SubscriptionType;

class InvoiceServiceTest {
    private final InvoiceRepository repository = mock(InvoiceRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final InvoiceService service = new InvoiceService(repository, outbox);

    @Test
    void keepsOriginalDayAfterShortMonth() {
        assertThat(InvoiceService.billingDate(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28)
        )).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(InvoiceService.billingDate(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 3, 31)
        )).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void doesNotCreateInvoiceBeforeActivation() {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getActivationDate()).thenReturn(LocalDate.of(2026, 8, 2));
        assertThat(service.createIfDue(subscription, LocalDate.of(2026, 8, 1))).isEmpty();
        verifyNoInteractions(repository, outbox);
    }

    @Test
    void skipsExistingInvoiceWhenInsertConflicts() {
        Subscription subscription = subscription(10L, LocalDate.of(2026, 8, 13));
        when(repository.insertIfAbsent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Integer.class)
        )).thenReturn(0);

        assertThat(service.createIfDue(subscription, LocalDate.of(2026, 8, 13)))
                .isEqualTo(Optional.empty());

        verify(repository).insertIfAbsent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Integer.class)
        );
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(outbox);
    }

    @Test
    void createsInvoiceAndOutboxEventWhenInsertSucceeds() {
        Subscription subscription = subscription(10L, LocalDate.of(2026, 8, 13));
        Invoice invoice = mock(Invoice.class);
        when(repository.insertIfAbsent(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Integer.class)
        )).thenReturn(1);
        when(repository.findBySubscriptionIdAndBillingDate(
                10L,
                LocalDate.of(2026, 8, 13)
        )).thenReturn(Optional.of(invoice));
        when(invoice.getId()).thenReturn(42L);
        when(invoice.getUserId()).thenReturn(7L);
        when(invoice.getBillingDate()).thenReturn(LocalDate.of(2026, 8, 13));
        when(invoice.getActivationDate()).thenReturn(LocalDate.of(2026, 8, 13));
        when(invoice.getSubscriptionTitle()).thenReturn("Basic");
        when(invoice.getPriceRubles()).thenReturn(100);

        assertThat(service.createIfDue(subscription, LocalDate.of(2026, 8, 13)))
                .contains(invoice);

        verify(outbox).add(any(OutboxEventType.class), any(InvoiceCreatedEvent.class));
    }

    private Subscription subscription(Long id, LocalDate activation) {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getId()).thenReturn(id);
        when(subscription.getUserId()).thenReturn(7L);
        when(subscription.getActivationDate()).thenReturn(activation);
        when(subscription.getType()).thenReturn(SubscriptionType.BASIC);
        return subscription;
    }
}
