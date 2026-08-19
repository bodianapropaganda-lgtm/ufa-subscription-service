package ru.subscription.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.subscription.cache.CachedInvoice;
import ru.subscription.cache.CachedSubscription;
import ru.subscription.cache.UserCacheService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCacheController {
    private final UserCacheService cache;

    @GetMapping("/{userId}")
    public UserResponse user(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return new UserResponse(
                cache.subscription(userId).orElse(null),
                cache.invoices(userId, page, size),
                page,
                size
        );
    }

    public record UserResponse(
            CachedSubscription activeSubscription,
            List<CachedInvoice> invoices,
            int page,
            int size
    ) {
    }
}
