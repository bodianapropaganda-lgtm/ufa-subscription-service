package ru.subscription.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.subscription.subscription.SubscriptionAlreadyActiveException;
import ru.subscription.subscription.SubscriptionNotFoundException;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void returnsConflictForActiveSubscription() {
        ResponseEntity<ApiExceptionHandler.ErrorResponse> response = handler.activeSubscription(
                new SubscriptionAlreadyActiveException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("User already has an active subscription");
    }

    @Test
    void returnsNotFoundForMissingSubscription() {
        ResponseEntity<ApiExceptionHandler.ErrorResponse> response = handler.subscriptionNotFound(
                new SubscriptionNotFoundException()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Active subscription was not found");
    }
}
