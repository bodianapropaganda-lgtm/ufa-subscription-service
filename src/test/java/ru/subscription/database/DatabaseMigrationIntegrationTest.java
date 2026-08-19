package ru.subscription.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .load()
                .migrate();
    }

    @Test
    void appliesSchemaAndPreventsTwoActiveSubscriptions() throws SQLException {
        try (Connection connection = openConnection()) {
            try (PreparedStatement insert = connection.prepareStatement("""
                    insert into subscriptions (user_id, type, activation_date)
                    values (?, ?, current_date)
                    """)) {
                insert.setLong(1, 101);
                insert.setString(2, "BASIC");
                insert.executeUpdate();

                insert.setLong(1, 101);
                insert.setString(2, "PRO");
                assertThatThrownBy(insert::executeUpdate).isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void preventsDuplicateInvoiceForSameBillingDate() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    insert into subscriptions (user_id, type, activation_date)
                    values (102, 'BASIC', current_date)
                    """);
            try (PreparedStatement insert = connection.prepareStatement("""
                    insert into invoices (
                        subscription_id,
                        user_id,
                        billing_date,
                        activation_date,
                        subscription_title,
                        price_rubles
                    )
                    values (
                        currval('subscriptions_id_seq'),
                        102,
                        current_date,
                        current_date,
                        'Basic',
                        100
                    )
                    """)) {
                insert.executeUpdate();
                assertThatThrownBy(insert::executeUpdate).isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void createsOneInvoiceAndOneOutboxEventDuringConcurrentInsert() throws Exception {
        long subscriptionId = createSubscription(103);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(() -> insertInvoiceAndOutbox(subscriptionId, ready, start));
            Future<?> second = executor.submit(() -> insertInvoiceAndOutbox(subscriptionId, ready, start));
            ready.await();
            start.countDown();
            first.get();
            second.get();
        }

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            assertThat(count(
                    statement,
                    "select count(*) from invoices where subscription_id = " + subscriptionId
            )).isEqualTo(1);
            assertThat(count(
                    statement,
                    "select count(*) from outbox_events where payload = 'invoice-' || "
                            + subscriptionId
            )).isEqualTo(1);
        }
    }

    private long createSubscription(long userId) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     insert into subscriptions (user_id, type, activation_date)
                     values (?, 'BASIC', current_date)
                     returning id
                     """)) {
            statement.setLong(1, userId);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertInvoiceAndOutbox(long subscriptionId, CountDownLatch ready, CountDownLatch start) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            ready.countDown();
            start.await();
            try (PreparedStatement invoice = connection.prepareStatement("""
                    insert into invoices (
                        subscription_id,
                        user_id,
                        billing_date,
                        activation_date,
                        subscription_title,
                        price_rubles
                    )
                    values (?, 103, current_date, current_date, 'Basic', 100)
                    on conflict (subscription_id, billing_date) do nothing returning id
                    """)) {
                invoice.setLong(1, subscriptionId);
                try (var resultSet = invoice.executeQuery()) {
                    if (resultSet.next()) {
                        try (PreparedStatement outbox = connection.prepareStatement("""
                                insert into outbox_events (
                                    id,
                                    type,
                                    payload,
                                    created_at,
                                    available_at
                                )
                                values (?, 'invoice.created', ?, ?, ?)
                                """)) {
                            outbox.setObject(1, UUID.randomUUID());
                            outbox.setString(2, "invoice-" + subscriptionId);
                            outbox.setObject(3, OffsetDateTime.now());
                            outbox.setObject(4, OffsetDateTime.now());
                            outbox.executeUpdate();
                        }
                    }
                }
            }
            connection.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long count(Statement statement, String sql) throws SQLException {
        try (var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }
}
