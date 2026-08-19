package ru.subscription.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class InvoiceRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private InvoiceRepository invoices;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void insertsInvoiceOnlyOnce() {
        LocalDate billingDate = LocalDate.of(2026, 8, 16);
        long subscriptionId = jdbcTemplate.queryForObject("""
                insert into subscriptions (user_id, type, activation_date)
                values (501, 'BASIC', ?)
                returning id
                """, Long.class, billingDate);

        int firstInsert = invoices.insertIfAbsent(
                subscriptionId,
                501L,
                billingDate,
                billingDate,
                "Basic",
                100
        );
        int repeatedInsert = invoices.insertIfAbsent(
                subscriptionId,
                501L,
                billingDate,
                billingDate,
                "Basic",
                100
        );

        assertThat(firstInsert).isEqualTo(1);
        assertThat(repeatedInsert).isZero();
        assertThat(invoices.findBySubscriptionIdAndBillingDate(subscriptionId, billingDate))
                .isPresent();
    }
}
