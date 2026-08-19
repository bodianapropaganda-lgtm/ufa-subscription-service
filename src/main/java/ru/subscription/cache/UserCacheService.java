package ru.subscription.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserCacheService {
    private static final DefaultRedisScript<Long> SAVE_SUBSCRIPTION = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current then
                redis.call('SET', KEYS[1], ARGV[1])
                return 1
            end
            local cached = cjson.decode(current)
            if (tonumber(cached.version) or 0) <= tonumber(ARGV[2]) then
                redis.call('SET', KEYS[1], ARGV[1])
                return 1
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> REMOVE_SUBSCRIPTION = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if not current then return 0 end
            local cached = cjson.decode(current)
            if cached.subscriptionId == tonumber(ARGV[1])
                    and (tonumber(cached.version) or 0) <= tonumber(ARGV[2]) then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public void saveSubscription(Long userId, CachedSubscription subscription) {
        String json = toJson(subscription);
        Long updated = execute(() -> redis.execute(
                SAVE_SUBSCRIPTION,
                List.of(subscriptionKey(userId)),
                json,
                String.valueOf(subscription.version())
        ));
        if (updated != null && updated > 0) {
            log.debug("Updated cached subscription for user {}", userId);
        } else {
            log.debug("Ignored stale subscription activation for user {}", userId);
        }
    }

    public void removeSubscription(Long userId, Long subscriptionId, long version) {
        Long removed = execute(() -> redis.execute(
                REMOVE_SUBSCRIPTION,
                List.of(subscriptionKey(userId)),
                String.valueOf(subscriptionId),
                String.valueOf(version)
        ));
        if (removed != null && removed > 0) {
            log.debug("Removed cached subscription for user {}", userId);
        } else {
            log.debug("Ignored stale subscription deactivation for user {}", userId);
        }
    }

    public void saveInvoice(Long userId, CachedInvoice invoice) {
        write(invoiceKey(userId, invoice.id()), invoice);
        execute(() -> redis.opsForZSet().add(
                invoicesKey(userId),
                String.valueOf(invoice.id()),
                invoice.billingDate().toEpochDay()
        ));
        log.debug("Cached invoice {} for user {}", invoice.id(), userId);
    }

    public Optional<CachedSubscription> subscription(Long userId) {
        return read(subscriptionKey(userId), CachedSubscription.class);
    }

    public List<CachedInvoice> invoices(Long userId, int page, int size) {
        long from = (long) page * size;
        Set<String> ids = execute(() -> redis.opsForZSet().reverseRange(
                invoicesKey(userId),
                from,
                from + size - 1
        ));
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<CachedInvoice> cachedInvoices = new ArrayList<>();
        for (String id : ids) {
            read(invoiceKey(userId, id), CachedInvoice.class)
                    .ifPresent(cachedInvoices::add);
        }
        log.debug(
                "Read {} cached invoices for user {}, page {}",
                cachedInvoices.size(),
                userId,
                page
        );
        return cachedInvoices;
    }

    private <T> void write(String key, T value) {
        String json = toJson(value);
        execute(() -> redis.opsForValue().set(key, json));
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String subscriptionKey(Long userId) {
        return "user:" + userId + ":subscription";
    }

    private String invoicesKey(Long userId) {
        return "user:" + userId + ":invoices";
    }

    private String invoiceKey(Long userId, Object invoiceId) {
        return "user:" + userId + ":invoice:" + invoiceId;
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        String value = execute(() -> redis.opsForValue().get(key));
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(fromJson(value, type));
    }

    private <T> T fromJson(String value, Class<T> type) {
        try {
            return mapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void execute(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException e) {
            log.warn("Redis operation failed: {}", e.getMessage());
            throw new RedisUnavailableException(e);
        }
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (RuntimeException e) {
            log.warn("Redis operation failed: {}", e.getMessage());
            throw new RedisUnavailableException(e);
        }
    }
}
