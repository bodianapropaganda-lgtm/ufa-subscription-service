package ru.subscription.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.subscription.subscription.Subscription;
import ru.subscription.subscription.SubscriptionService;
import ru.subscription.subscription.SubscriptionType;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionService service;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> activate(
            @Valid @RequestBody ActivateSubscriptionRequest request
    ) {
        Subscription subscription = service.activate(
                request.userId(),
                request.type(),
                request.activationDate()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SubscriptionResponse.from(subscription));
    }

    @DeleteMapping
    public ResponseEntity<Void> deactivate(
            @Valid @RequestBody DeactivateSubscriptionRequest request
    ) {
        service.deactivate(request.userId(), request.type());
        return ResponseEntity.noContent().build();
    }

    public record ActivateSubscriptionRequest(
            @NotNull @Positive Long userId,
            @NotNull SubscriptionType type,
            @NotNull LocalDate activationDate
    ) {
    }

    public record DeactivateSubscriptionRequest(
            @NotNull @Positive Long userId,
            @NotNull SubscriptionType type
    ) {
    }

    public record SubscriptionResponse(
            Long id,
            Long userId,
            SubscriptionType type,
            LocalDate activationDate
    ) {
        static SubscriptionResponse from(Subscription subscription) {
            return new SubscriptionResponse(
                    subscription.getId(),
                    subscription.getUserId(),
                    subscription.getType(),
                    subscription.getActivationDate()
            );
        }
    }
}
