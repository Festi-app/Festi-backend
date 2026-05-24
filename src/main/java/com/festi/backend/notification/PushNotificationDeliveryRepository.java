package com.festi.backend.notification;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushNotificationDeliveryRepository extends JpaRepository<PushNotificationDelivery, UUID> {

    Optional<PushNotificationDelivery> findFirstByEventIdAndEndpointOrderByCreatedAtDesc(
            UUID eventId, String endpoint);
}
