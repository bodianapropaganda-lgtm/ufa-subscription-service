package ru.subscription.subscription;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndDeactivationDateIsNull(Long userId);

    List<Subscription> findByDeactivationDateIsNullAndActivationDateLessThanEqual(LocalDate date);

    @Query(value = "select nextval('subscription_event_sequence')", nativeQuery = true)
    long nextSubscriptionEventSequence();
}
