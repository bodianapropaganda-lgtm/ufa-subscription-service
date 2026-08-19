package ru.subscription.outbox;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
            select * from outbox_events
            where published_at is null and available_at <= :now
              and (locked_until is null or locked_until < :now)
            order by created_at
            limit 10 for update skip locked
            """, nativeQuery = true)
    List<OutboxEvent> lockReadyEvents(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            update OutboxEvent event
            set event.publishedAt = :publishedAt,
                event.lockedBy = null,
                event.lockedUntil = null
            where event.id = :id
              and event.publishedAt is null
              and event.lockedBy = :publisherId
            """)
    int markPublished(
            @Param("id") UUID id,
            @Param("publisherId") String publisherId,
            @Param("publishedAt") OffsetDateTime publishedAt
    );
}
