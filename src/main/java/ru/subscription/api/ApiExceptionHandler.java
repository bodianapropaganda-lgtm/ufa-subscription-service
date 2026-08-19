package ru.subscription.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.subscription.cache.RedisUnavailableException;
import ru.subscription.subscription.SubscriptionAlreadyActiveException;
import ru.subscription.subscription.SubscriptionNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(SubscriptionAlreadyActiveException.class)
    ResponseEntity<ErrorResponse> activeSubscription(SubscriptionAlreadyActiveException e) {
        return response(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    ResponseEntity<ErrorResponse> subscriptionNotFound(SubscriptionNotFoundException e) {
        return response(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> invalid(IllegalArgumentException e) {
        return response(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e) {
        return response(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> methodValidation(HandlerMethodValidationException e) {
        return response(HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    @ExceptionHandler(RedisUnavailableException.class)
    ResponseEntity<ErrorResponse> redis(RedisUnavailableException e) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Cache is temporarily unavailable");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                message
        );
        return ResponseEntity.status(status).body(error);
    }

    record ErrorResponse(Instant timestamp, int status, String message) {
    }
}
