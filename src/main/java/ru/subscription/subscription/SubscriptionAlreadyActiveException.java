package ru.subscription.subscription;

public class SubscriptionAlreadyActiveException extends RuntimeException {
    public SubscriptionAlreadyActiveException() {
        super("User already has an active subscription");
    }
}
