package ru.subscription.outbox;

public class UnsupportedEventTypeException extends RuntimeException {
    private final String routingKey;

    public UnsupportedEventTypeException(String routingKey) {
        super("Unsupported event routing key: " + routingKey);
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }
}
