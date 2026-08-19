package ru.subscription.cache;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import ru.subscription.outbox.InvoiceCreatedEvent;
import ru.subscription.outbox.OutboxEventType;
import ru.subscription.outbox.SubscriptionActivatedEvent;
import ru.subscription.outbox.UnsupportedEventTypeException;
import ru.subscription.subscription.SubscriptionType;

class CacheEventListenerTest {
    @Test
    void updatesSubscription() throws Exception {
        UserCacheService cache = mock(UserCacheService.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        SubscriptionActivatedEvent event = new SubscriptionActivatedEvent(
                UUID.randomUUID(),
                7L,
                9L,
                3L,
                SubscriptionType.BASIC,
                LocalDate.of(2026, 8, 13)
        );

        new CacheEventListener(cache, mapper).handle(
                message(
                        mapper.writeValueAsString(event),
                        OutboxEventType.SUBSCRIPTION_ACTIVATED
                )
        );

        verify(cache).saveSubscription(
                9L,
                new CachedSubscription(
                        3L,
                        7L,
                        "BASIC",
                        LocalDate.of(2026, 8, 13)
                )
        );
    }

    @Test
    void passesRepeatedInvoiceEventToCacheService() throws Exception {
        UserCacheService cache = mock(UserCacheService.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        InvoiceCreatedEvent event = new InvoiceCreatedEvent(
                UUID.randomUUID(),
                9L,
                3L,
                1L,
                LocalDate.now(),
                LocalDate.now(),
                "Basic",
                100
        );
        CacheEventListener listener = new CacheEventListener(cache, mapper);
        Message message = message(mapper.writeValueAsString(event), OutboxEventType.INVOICE_CREATED);
        listener.handle(message);
        listener.handle(message);
        verify(cache, times(2)).saveInvoice(
                9L,
                new CachedInvoice(
                        1L,
                        event.billingDate(),
                        event.activationDate(),
                        "Basic",
                        100
                )
        );
    }

    @Test
    void throwsForUnknownRoutingKey() {
        UserCacheService cache = mock(UserCacheService.class);
        CacheEventListener listener = new CacheEventListener(cache, new ObjectMapper().findAndRegisterModules());
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey("subscription.unknown");

        Message message = new Message(
                "{}".getBytes(StandardCharsets.UTF_8),
                properties
        );

        assertThatThrownBy(() -> listener.handle(message))
                .isInstanceOf(UnsupportedEventTypeException.class)
                .hasMessageContaining("subscription.unknown");

        verifyNoInteractions(cache);
    }

    private Message message(String payload, OutboxEventType eventType) {
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(eventType.routingKey());
        return new Message(payload.getBytes(StandardCharsets.UTF_8), properties);
    }
}
