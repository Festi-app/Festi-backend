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
    @Query(value = """
            UPDATE waiting_notification_events
            SET status = 'RETRY_WAIT'::waiting_notification_event_status,
                available_at = :availableAt,
                processing_started_at = null,
                failure_reason = 'Processing lease expired.'
            WHERE status = 'PROCESSING'::waiting_notification_event_status
              AND processing_started_at < :expiredBefore
            """, nativeQuery = true)
    int requeueExpiredProcessing(@Param("expiredBefore") OffsetDateTime expiredBefore,
                                 @Param("availableAt") OffsetDateTime availableAt);
}
