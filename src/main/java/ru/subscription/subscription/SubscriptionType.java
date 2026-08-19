package ru.subscription.subscription;

public enum SubscriptionType {
    BASIC("Basic", 100),
    PRO("PRO", 200);

    private final String title;
    private final int priceRubles;

    SubscriptionType(String title, int priceRubles) {
        this.title = title;
        this.priceRubles = priceRubles;
    }

    public String title() {
        return title;
    }

    public int priceRubles() {
        return priceRubles;
    }
}
