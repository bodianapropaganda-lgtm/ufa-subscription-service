package ru.subscription.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import ru.subscription.invoice.InvoiceService;
import ru.subscription.outbox.OutboxService;
import ru.subscription.outbox.OutboxEventType;

class SubscriptionServiceTest {
    private final SubscriptionRepository repository = mock(SubscriptionRepository.class);
    private final InvoiceService invoices = mock(InvoiceService.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final SubscriptionService service = new SubscriptionService(repository, invoices, outbox);

    @Test
    void activatesSubscription() {
        when(repository.findByUserIdAndDeactivationDateIsNull(1L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Subscription result = service.activate(1L, SubscriptionType.BASIC, today);
        assertThat(result.getType()).isEqualTo(SubscriptionType.BASIC);
        verify(outbox).add(any(), any(), any());
        verify(invoices).createIfDue(result, today);
    }

    @Test
    void doesNotCreateInvoiceForFutureActivation() {
        when(repository.findByUserIdAndDeactivationDateIsNull(1L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));
        service.activate(1L, SubscriptionType.BASIC, LocalDate.now(ZoneOffset.UTC).plusDays(1));
        verifyNoInteractions(invoices);
    }

    @Test
    void rejectsSecondActiveSubscription() {
        when(repository.findByUserIdAndDeactivationDateIsNull(1L))
                .thenReturn(Optional.of(mock(Subscription.class)));

        assertThatThrownBy(() -> service.activate(
                1L,
                SubscriptionType.PRO,
                LocalDate.now(ZoneOffset.UTC)
        )).isInstanceOf(SubscriptionAlreadyActiveException.class);
    }

    @Test
    void rejectsPastActivationDate() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.activate(
                1L,
                SubscriptionType.PRO,
                LocalDate.now(ZoneOffset.UTC).minusDays(1)
        ));
    }

    @Test
    void translatesDatabaseRaceToBusinessError() {
        when(repository.findByUserIdAndDeactivationDateIsNull(1L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> service.activate(
                1L,
                SubscriptionType.PRO,
                LocalDate.now(ZoneOffset.UTC)
        )).isInstanceOf(SubscriptionAlreadyActiveException.class);
    }

    @Test
    void deactivatesMatchingSubscription() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Subscription subscription = new Subscription(1L, SubscriptionType.BASIC, today);
        when(repository.findByUserIdAndDeactivationDateIsNull(1L)).thenReturn(Optional.of(subscription));
        service.deactivate(1L, SubscriptionType.BASIC);
        assertThat(subscription.getDeactivationDate()).isEqualTo(today);
        verify(outbox).add(any(), any());
    }

    @Test
    void reportsMissingSubscription() {
        when(repository.findByUserIdAndDeactivationDateIsNull(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deactivate(1L, SubscriptionType.BASIC))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    @Test
    void delaysFutureDeactivationEventUntilActivationDate() {
        LocalDate activationDate = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        Subscription subscription = new Subscription(1L, SubscriptionType.BASIC, activationDate);
        when(repository.findByUserIdAndDeactivationDateIsNull(1L)).thenReturn(Optional.of(subscription));

        service.deactivate(1L, SubscriptionType.BASIC);

        verify(outbox).add(
                eq(OutboxEventType.SUBSCRIPTION_DEACTIVATED),
                any(),
                eq(activationDate.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime())
        );
    }
}
