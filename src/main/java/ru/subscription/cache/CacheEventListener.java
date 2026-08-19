package ru.subscription.cache;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.subscription.outbox.InvoiceCreatedEvent;
import ru.subscription.outbox.OutboxEventType;
import ru.subscription.outbox.SubscriptionActivatedEvent;
import ru.subscription.outbox.SubscriptionDeactivatedEvent;
import ru.subscription.outbox.UnsupportedEventTypeException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CacheEventListener {
    private final UserCacheService cache;
    private final ObjectMapper mapper;

    @RabbitListener(queues = "subscription.cache")
    public void handle(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            OutboxEventType eventType = OutboxEventType.fromRoutingKey(routingKey);
            switch (eventType) {
                case SUBSCRIPTION_ACTIVATED -> {
                    SubscriptionActivatedEvent event = mapper.readValue(
                            payload,
                            SubscriptionActivatedEvent.class
                    );
                    CachedSubscription subscription = new CachedSubscription(
                            event.subscriptionId(),
                            event.sequence(),
                            event.subscriptionType().name(),
                            event.activationDate()
                    );
                    cache.saveSubscription(event.userId(), subscription);
                    log.debug("Cached activated subscription for user {}", event.userId());
                }
                case SUBSCRIPTION_DEACTIVATED -> {
                    SubscriptionDeactivatedEvent event = mapper.readValue(
                            payload,
                            SubscriptionDeactivatedEvent.class
                    );
                    cache.removeSubscription(
                            event.userId(),
                            event.subscriptionId(),
                            event.sequence()
                    );
                    log.debug("Cached subscription deactivation for user {}", event.userId());
                }
                case INVOICE_CREATED -> {
                    InvoiceCreatedEvent event = mapper.readValue(
                            payload,
                            InvoiceCreatedEvent.class
                    );
                    CachedInvoice invoice = new CachedInvoice(
                            event.invoiceId(),
                            event.billingDate(),
                            event.activationDate(),
                            event.subscriptionTitle(),
                            event.priceRubles()
                    );
                    cache.saveInvoice(event.userId(), invoice);
                    log.debug("Cached invoice {} for user {}", event.invoiceId(), event.userId());
                }
                default -> throw new UnsupportedEventTypeException(eventType.routingKey());
            }
        } catch (UnsupportedEventTypeException e) {
            log.error("Unsupported event routing key {}", e.getRoutingKey());
            throw e;
        } catch (IOException e) {
            log.error("Cannot deserialize event with routing key {}", routingKey);
            throw new IllegalArgumentException("Invalid event payload", e);
        }
    }
}
