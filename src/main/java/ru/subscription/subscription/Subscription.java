package ru.subscription.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private SubscriptionType type;
    private LocalDate activationDate;
    private LocalDate deactivationDate;

    public Subscription(Long userId, SubscriptionType type, LocalDate activationDate) {
        this.userId = userId;
        this.type = type;
        this.activationDate = activationDate;
    }

    public void deactivate(LocalDate date) {
        this.deactivationDate = date;
    }

}
