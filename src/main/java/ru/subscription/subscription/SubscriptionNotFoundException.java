package ru.subscription.subscription;

public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException() {
        super("Active subscription was not found");
    }
}
