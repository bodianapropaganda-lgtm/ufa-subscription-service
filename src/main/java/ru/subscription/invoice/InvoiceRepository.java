package ru.subscription.invoice;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Modifying
    @Query(value = """
            insert into invoices (
                subscription_id,
                user_id,
                billing_date,
                activation_date,
                subscription_title,
                price_rubles
            )
            values (
                :subscriptionId,
                :userId,
                :billingDate,
                :activationDate,
                :subscriptionTitle,
                :priceRubles
            )
            on conflict (subscription_id, billing_date) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("subscriptionId") Long subscriptionId,
                       @Param("userId") Long userId,
                       @Param("billingDate") LocalDate billingDate,
                       @Param("activationDate") LocalDate activationDate,
                       @Param("subscriptionTitle") String subscriptionTitle,
                       @Param("priceRubles") int priceRubles);

    Optional<Invoice> findBySubscriptionIdAndBillingDate(Long subscriptionId, LocalDate billingDate);
}
