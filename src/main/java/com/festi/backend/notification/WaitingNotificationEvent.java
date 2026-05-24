package com.festi.backend.notification;

import com.festi.backend.waiting.Waiting;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "waiting_notification_events")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WaitingNotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiting_id", nullable = false)
    private Waiting waiting;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(length = 500)
    private String icon;

    @Column(nullable = false, length = 500)
    private String url;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "waiting_notification_event_status")
    private WaitingNotificationEventStatus status = WaitingNotificationEventStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private OffsetDateTime availableAt;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public WaitingNotificationEvent(Waiting waiting, String eventType, String title, String body,
                                    String icon, String url, OffsetDateTime availableAt) {
        this.waiting = waiting;
        this.eventType = eventType;
        this.title = title;
        this.body = body;
        this.icon = icon;
        this.url = url;
        this.availableAt = availableAt;
    }

    public void markProcessing(OffsetDateTime processingStartedAt) {
        this.status = WaitingNotificationEventStatus.PROCESSING;
        this.attemptCount++;
        this.processingStartedAt = processingStartedAt;
        this.failureReason = null;
    }

    public void scheduleRetry(OffsetDateTime availableAt, String failureReason) {
        this.status = WaitingNotificationEventStatus.RETRY_WAIT;
        this.availableAt = availableAt;
        this.processingStartedAt = null;
        this.failureReason = failureReason;
    }

    public void markCompleted(OffsetDateTime processedAt) {
        this.status = WaitingNotificationEventStatus.COMPLETED;
        this.processingStartedAt = null;
        this.processedAt = processedAt;
        this.failureReason = null;
    }

    public void markFailed(OffsetDateTime processedAt, String failureReason) {
        this.status = WaitingNotificationEventStatus.FAILED;
        this.processingStartedAt = null;
        this.processedAt = processedAt;
        this.failureReason = failureReason;
    }
}
