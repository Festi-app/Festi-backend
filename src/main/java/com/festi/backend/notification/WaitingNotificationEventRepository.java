package com.festi.backend.notification;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitingNotificationEventRepository extends JpaRepository<WaitingNotificationEvent, UUID> {

    @Query(value = """
            SELECT *
            FROM waiting_notification_events
            WHERE status IN ('PENDING', 'RETRY_WAIT')
              AND available_at <= :availableAt
            ORDER BY created_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<WaitingNotificationEvent> findNextAvailableForUpdate(@Param("availableAt") OffsetDateTime availableAt);

    @Modifying
    @Query("""
            UPDATE WaitingNotificationEvent event
            SET event.status = com.festi.backend.notification.WaitingNotificationEventStatus.RETRY_WAIT,
                event.availableAt = :availableAt,
                event.processingStartedAt = null,
                event.failureReason = 'Processing lease expired.'
            WHERE event.status = com.festi.backend.notification.WaitingNotificationEventStatus.PROCESSING
              AND event.processingStartedAt < :expiredBefore
            """)
    int requeueExpiredProcessing(@Param("expiredBefore") OffsetDateTime expiredBefore,
                                 @Param("availableAt") OffsetDateTime availableAt);
}
