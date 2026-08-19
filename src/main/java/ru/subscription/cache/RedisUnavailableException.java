package ru.subscription.cache;

public class RedisUnavailableException extends RuntimeException {
    public RedisUnavailableException(Throwable cause) {
        super("Redis is temporarily unavailable", cause);
    }
}
