package com.festi.backend.notification;

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
@Table(name = "push_notification_deliveries")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushNotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private WaitingNotificationEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private PushSubscription subscription;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "push_notification_delivery_status")
    private PushNotificationDeliveryStatus status = PushNotificationDeliveryStatus.PENDING;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "attempted_at")
    private OffsetDateTime attemptedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PushNotificationDelivery(WaitingNotificationEvent event, PushSubscription subscription) {
        this.event = event;
        this.subscription = subscription;
        this.endpoint = subscription.getEndpoint();
    }

    public void markSent(int responseStatus, OffsetDateTime attemptedAt) {
        this.status = PushNotificationDeliveryStatus.SENT;
        this.responseStatus = responseStatus;
        this.failureReason = null;
        this.attemptedAt = attemptedAt;
    }

    public void markFailed(Integer responseStatus, String failureReason, OffsetDateTime attemptedAt) {
        this.status = PushNotificationDeliveryStatus.FAILED;
        this.responseStatus = responseStatus;
        this.failureReason = failureReason;
        this.attemptedAt = attemptedAt;
    }
}
