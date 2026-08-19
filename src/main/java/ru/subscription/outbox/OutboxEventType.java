package ru.subscription.outbox;

public enum OutboxEventType {
    SUBSCRIPTION_ACTIVATED("subscription.activated"),
    SUBSCRIPTION_DEACTIVATED("subscription.deactivated"),
    INVOICE_CREATED("invoice.created");

    private final String routingKey;

    OutboxEventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String routingKey() {
        return routingKey;
    }

    public static OutboxEventType fromRoutingKey(String routingKey) {
        for (OutboxEventType value : values()) {
            if (value.routingKey.equals(routingKey)) {
                return value;
            }
        }
        throw new UnsupportedEventTypeException(routingKey);
    }
}
