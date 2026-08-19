package ru.subscription.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id
    @Getter
    private UUID id;
    @Getter
    private String type;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Getter
    private String payload;
    private OffsetDateTime createdAt;
    @Getter
    private OffsetDateTime availableAt;
    private OffsetDateTime publishedAt;
    private String lockedBy;
    private OffsetDateTime lockedUntil;

    public OutboxEvent(String type, String payload, OffsetDateTime createdAt, OffsetDateTime availableAt) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.payload = payload;
        this.createdAt = createdAt;
        this.availableAt = availableAt;
    }

    public void markPublished(OffsetDateTime at) {
        publishedAt = at;
    }

    public void claim(String publisherId, OffsetDateTime until) {
        lockedBy = publisherId;
        lockedUntil = until;
    }

}
