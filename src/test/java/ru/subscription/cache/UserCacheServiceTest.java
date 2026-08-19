package ru.subscription.cache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

class UserCacheServiceTest {
    @Test
    void storesInvoiceIdAsSortedSetMember() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> invoices = mock(ZSetOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForZSet()).thenReturn(invoices);

        CachedInvoice invoice = new CachedInvoice(
                42L,
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 14),
                "Basic",
                100
        );
        UserCacheService cache = new UserCacheService(
                redis,
                new ObjectMapper().findAndRegisterModules()
        );

        cache.saveInvoice(7L, invoice);

        verify(invoices).add("user:7:invoices", "42", LocalDate.of(2026, 8, 14).toEpochDay());
    }
}
